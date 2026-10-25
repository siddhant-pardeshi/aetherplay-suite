package io.github.siddhantpardeshi.aetherplay.lab.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.github.siddhantpardeshi.aetherplay.model.Protocol
import io.github.siddhantpardeshi.aetherplay.protocol.BridgeContract
import io.github.siddhantpardeshi.aetherplay.protocol.HandshakeInfo
import io.github.siddhantpardeshi.aetherplay.protocol.IBridgeBroker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ConnState {
    data object Disconnected : ConnState
    data object Connecting : ConnState
    data class Connected(val info: HandshakeInfo) : ConnState
    data class Incompatible(val reason: String) : ConnState
}

/**
 * Manages the binder connection to AetherPlay Space: bind, handshake,
 * death-recovery with bounded retries, and suspend-wrapped broker calls.
 */
class BridgeClient(
    private val context: Context,
    private val scope: CoroutineScope,
    private val labBuild: String,
) {

    private val _state = MutableStateFlow<ConnState>(ConnState.Disconnected)
    val state: StateFlow<ConnState> = _state

    @Volatile
    var broker: IBridgeBroker? = null
        private set

    private var bound = false
    private var retries = 0
    private var deathRecipient: IBinder.DeathRecipient? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            broker = IBridgeBroker.Stub.asInterface(service)
            deathRecipient = IBinder.DeathRecipient {
                broker = null
                _state.value = ConnState.Disconnected
                scheduleRebind()
            }
            deathRecipient?.let { recipient -> runCatching { service?.linkToDeath(recipient, 0) } }
            scope.launch { handshake() }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            broker = null
        }
    }

    fun connect() {
        if (bound) return
        _state.value = ConnState.Connecting
        val intent = Intent(BridgeContract.ACTION).setPackage(BridgeContract.SPACE_PACKAGE)
        val ok = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (_: Exception) {
            false
        }
        if (!ok) {
            _state.value = ConnState.Disconnected
        } else {
            bound = true
        }
    }

    fun disconnect() {
        runCatching { deathRecipient?.let { broker?.asBinder()?.unlinkToDeath(it, 0) } }
        runCatching { if (bound) context.unbindService(connection) }
        bound = false
        broker = null
        _state.value = ConnState.Disconnected
    }

    private fun scheduleRebind() {
        if (retries >= 3) return
        retries += 1
        scope.launch {
            delay(1200L * retries)
            connect()
        }
    }

    private suspend fun handshake() {
        val b = broker ?: return
        try {
            val info = withContext(Dispatchers.IO) {
                b.handshake(labBuild, Protocol.MAJOR, Protocol.MINOR)
            }
            retries = 0
            val verdict = Protocol.negotiate(Protocol.MAJOR, Protocol.MINOR, info.protocolMajor, info.protocolMinor)
            _state.value = when (verdict) {
                is Protocol.Verdict.Compatible -> ConnState.Connected(info)
                is Protocol.Verdict.MajorMismatch -> ConnState.Incompatible(
                    "AetherPlay Space uses bridge protocol ${info.protocolMajor}.x, Lab speaks ${Protocol.MAJOR}.x. Update both apps to a matching pair."
                )
            }
        } catch (_: Exception) {
            _state.value = ConnState.Disconnected
        }
    }

    suspend fun <T> call(block: suspend (IBridgeBroker) -> T): T? {
        val b = broker ?: run { connect(); return null }
        return try {
            withContext(Dispatchers.IO) { block(b) }
        } catch (_: Exception) {
            _state.value = ConnState.Disconnected
            scheduleRebind()
            null
        }
    }
}

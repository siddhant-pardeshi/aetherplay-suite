package io.github.siddhantpardeshi.aetherplay.space

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.siddhantpardeshi.aetherplay.protocol.BridgeContract
import io.github.siddhantpardeshi.aetherplay.protocol.SessionStatus
import io.github.siddhantpardeshi.aetherplay.space.bridge.SessionBus
import io.github.siddhantpardeshi.aetherplay.space.data.SpaceInfo
import io.github.siddhantpardeshi.aetherplay.space.data.SpaceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpaceViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SpaceStore(app)

    private val _spaces = MutableStateFlow(store.list())
    val spaces: StateFlow<List<SpaceInfo>> = _spaces

    val session: StateFlow<SessionStatus> = SessionBus.state

    private val _labInstalled = MutableStateFlow(false)
    val labInstalled: StateFlow<Boolean> = _labInstalled

    private val _storageBytes = MutableStateFlow(0L)
    val storageBytes: StateFlow<Long> = _storageBytes

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _spaces.value = store.list()
            _storageBytes.value = store.storageBytes()
            _labInstalled.value = isPackageInstalled(getApplication(), BridgeContract.LAB_PACKAGE)
        }
    }

    fun createSpace(name: String) {
        store.create(name)
        refresh()
    }

    fun renameSpace(key: String, name: String) {
        store.rename(key, name)
        refresh()
    }

    fun deleteSpace(key: String) {
        store.delete(key)
        refresh()
    }

    fun spaceName(key: String?): String = _spaces.value.firstOrNull { it.key == key }?.name ?: "—"

    private fun isPackageInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Exception) {
        false
    }
}

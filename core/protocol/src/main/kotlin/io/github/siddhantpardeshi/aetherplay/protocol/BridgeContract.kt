package io.github.siddhantpardeshi.aetherplay.protocol

/**
 * Constants shared by both apps. The service action, the signature-guarded
 * permission and both application ids live here so neither app hard-codes
 * strings that can drift.
 */
object BridgeContract {
    const val LAB_PACKAGE = "io.github.siddhantpardeshi.aetherplay.lab"
    const val SPACE_PACKAGE = "io.github.siddhantpardeshi.aetherplay.space"
    const val ACTION = "io.github.siddhantpardeshi.aetherplay.BRIDGE"
    const val PERMISSION = "io.github.siddhantpardeshi.aetherplay.BRIDGE"
    const val PROTOCOL_MAJOR = 1
    const val PROTOCOL_MINOR = 0
}

object SessionState {
    const val NONE = 0
    const val STARTING = 1
    const val RUNNING = 2
    const val STOPPING = 3
    const val FAILED = 4
}

object SearchPhase {
    const val IDLE = 0
    const val QUEUED = 1
    const val SCANNING = 2
    const val DONE = 3
    const val CANCELLED = 4
    const val FAILED = 5
}

object GuestKind {
    const val TRAINING_COOPERATIVE = 0
    const val IMPORTED_GUEST = 1
}

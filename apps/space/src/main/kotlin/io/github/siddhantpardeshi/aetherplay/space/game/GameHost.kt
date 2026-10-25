package io.github.siddhantpardeshi.aetherplay.space.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-wide holder so the service, broker and game activity share one game. */
object GameHost {

    @Volatile
    var core: GameCore? = null
        private set

    private val fallback = MutableStateFlow(GameState())

    val state: StateFlow<GameState>
        get() = core?.state ?: fallback

    fun attach(core: GameCore) {
        this.core = core
    }

    fun detach(save: Boolean) {
        if (save) core?.save()
        core = null
        fallback.value = GameState()
    }

    val isRunning: Boolean get() = core != null
}

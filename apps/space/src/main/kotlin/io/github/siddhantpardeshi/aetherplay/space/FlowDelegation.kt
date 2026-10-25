package io.github.siddhantpardeshi.aetherplay.space

import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KProperty

/**
 * StateFlow property delegation (`val x by flow`). Newer compose-runtime
 * builds dropped their built-in operator, so the suite ships its own.
 */
operator fun <T> StateFlow<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

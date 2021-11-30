package io.novafoundation.nova.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

// TODO waiting for multiple receivers feature, probably in Kotlin 1.7
interface WithCoroutineScopeExtensions {

    val coroutineScope: CoroutineScope

    fun <T> Flow<T>.share() = shareIn(coroutineScope, started = SharingStarted.Eagerly, replay = 1)
}

fun WithCoroutineScopeExtensions(coroutineScope: CoroutineScope) = object : WithCoroutineScopeExtensions {
    override val coroutineScope: CoroutineScope = coroutineScope
}

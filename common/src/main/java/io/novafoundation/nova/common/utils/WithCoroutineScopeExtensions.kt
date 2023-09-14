package io.novafoundation.nova.common.utils

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

// TODO waiting for multiple receivers feature, probably in Kotlin 1.7
interface WithCoroutineScopeExtensions {

    val coroutineScope: CoroutineScope

    fun <T> Flow<T>.share(
        started: SharingStarted = SharingStarted.Eagerly
    ) = shareIn(coroutineScope, started = started, replay = 1)

    fun <T> Flow<T>.shareLazily() = shareIn(coroutineScope, started = SharingStarted.Lazily, replay = 1)

    fun <T> Flow<T>.shareInBackground(
        started: SharingStarted = SharingStarted.Eagerly
    ) = inBackground().share(started)

    fun <T> Flow<T>.shareWhileSubscribed() = share(SharingStarted.WhileSubscribed())

    fun <T> Flow<T>.asLiveData(): LiveData<T> {
        return asLiveData(coroutineScope)
    }
}

fun WithCoroutineScopeExtensions(coroutineScope: CoroutineScope) = object : WithCoroutineScopeExtensions {
    override val coroutineScope: CoroutineScope = coroutineScope
}

context(CoroutineScope)
fun <T> Flow<T>.share(started: SharingStarted = SharingStarted.Eagerly): SharedFlow<T> {
    return shareIn(this@CoroutineScope, started = started, replay = 1)
}

context(CoroutineScope)
fun <T> Flow<T>.shareInBackground(started: SharingStarted = SharingStarted.Eagerly): SharedFlow<T> {
    return inBackground().share(started)
}

package io.novafoundation.nova.common.utils.splash

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

interface SplashPassedObserver {

    val isSplashPassed: Flow<Boolean>

    fun setSplashPassed()
}

suspend fun SplashPassedObserver.awaitSplashPassed() {
    isSplashPassed.first { allowed -> allowed == true }
}

internal class RealSplashPassedObserver : SplashPassedObserver, CoroutineScope by CoroutineScope(Dispatchers.Default) {

    override val isSplashPassed = MutableStateFlow(false)

    override fun setSplashPassed() {
        isSplashPassed.value = true
    }
}

package io.novafoundation.nova.core.updater

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform

/**
 * We do not want this extension to be visible outside of update system
 * So, we put it into marker interface, which will allow to reach it in consumers code
 */
interface SideEffectScope {

    fun <T> Flow<T>.noSideAffects(): Flow<Updater.SideEffect> = transform { }
}

interface UpdateScope<S> {

    fun invalidationFlow(): Flow<S?>
}

object GlobalScope : UpdateScope<Unit> {

    override fun invalidationFlow() = flowOf(Unit)
}

interface GlobalScopeUpdater : Updater<Unit> {

    override val scope
        get() = GlobalScope
}

interface Updater<V> : SideEffectScope {

    val requiredModules: List<String>

    val scope: UpdateScope<V>

    /**
     * Implementations should be aware of cancellation
     */
    suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: V,
    ): Flow<SideEffect>

    interface SideEffect
}

interface UpdateSystem {

    fun start(): Flow<Updater.SideEffect>
}

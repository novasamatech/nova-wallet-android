package io.novafoundation.nova.runtime.network.updaters.multiChain

import io.novafoundation.nova.core.updater.Updater

class AsSharedStateUpdater<T>(private val delegate: Updater<T>): Updater<T> by delegate, SharedStateBasedUpdater<T>

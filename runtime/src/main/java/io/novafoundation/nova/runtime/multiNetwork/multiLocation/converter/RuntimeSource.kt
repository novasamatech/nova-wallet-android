package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import kotlinx.coroutines.Deferred

sealed interface RuntimeSource {

    suspend fun getRuntime(): RuntimeSnapshot

    class Sync(private val runtimeSnapshot: RuntimeSnapshot): RuntimeSource {

        override suspend fun getRuntime(): RuntimeSnapshot {
            return runtimeSnapshot
        }
    }

    class Async(private val runtimeSnapshotAsync: Deferred<RuntimeSnapshot>): RuntimeSource {

        override suspend fun getRuntime(): RuntimeSnapshot {
            return runtimeSnapshotAsync.await()
        }
    }
}

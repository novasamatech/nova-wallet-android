package io.novafoundation.nova.common.data.holders

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

interface RuntimeHolder {

    suspend fun runtime(): RuntimeSnapshot
}

suspend inline fun <T> RuntimeHolder.useRuntime(block: (RuntimeSnapshot) -> T) = block(runtime())

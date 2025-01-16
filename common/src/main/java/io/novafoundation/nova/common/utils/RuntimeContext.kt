package io.novafoundation.nova.common.utils

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata

interface RuntimeContext {

    val runtime: RuntimeSnapshot
}

val RuntimeContext.metadata: RuntimeMetadata
    get() = runtime.metadata

fun RuntimeContext(runtime: RuntimeSnapshot): RuntimeContext {
    return InlineRuntimeContext(runtime)
}

@JvmInline
private value class InlineRuntimeContext(override val runtime: RuntimeSnapshot) : RuntimeContext



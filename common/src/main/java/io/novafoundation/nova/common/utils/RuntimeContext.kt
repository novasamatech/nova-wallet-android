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

inline fun <R> RuntimeSnapshot.provideContext(action: RuntimeContext.() -> R): R {
    return with(RuntimeContext(this)) {
        action()
    }
}

@JvmInline
private value class InlineRuntimeContext(override val runtime: RuntimeSnapshot) : RuntimeContext

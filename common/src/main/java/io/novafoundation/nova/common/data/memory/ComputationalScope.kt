package io.novafoundation.nova.common.data.memory

import kotlinx.coroutines.CoroutineScope

/**
 * A specialization of `CoroutineScope` to avoid context receiver pollution when used as `context(ComputationalScope)`
 */
interface ComputationalScope : CoroutineScope

fun ComputationalScope(scope: CoroutineScope): ComputationalScope = InlineComputationalScope(scope)

@JvmInline
private value class InlineComputationalScope(val scope: CoroutineScope): ComputationalScope, CoroutineScope by scope

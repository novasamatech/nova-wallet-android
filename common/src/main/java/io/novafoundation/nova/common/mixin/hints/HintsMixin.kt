package io.novafoundation.nova.common.mixin.hints

import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface HintsMixin {

    val hints: Flow<List<String>>
}

abstract class ConstantHintsMixin(
    coroutineScope: CoroutineScope
) : HintsMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    abstract suspend fun getHints(): List<String>

    override val hints: Flow<List<String>> = flowOf {
        getHints()
    }
        .inBackground()
        .share()
}

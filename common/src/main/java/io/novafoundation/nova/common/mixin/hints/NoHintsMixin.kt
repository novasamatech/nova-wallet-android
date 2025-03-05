package io.novafoundation.nova.common.mixin.hints

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NoHintsMixin : HintsMixin {

    override val hintsFlow: Flow<List<CharSequence>> = flowOf(emptyList())
}

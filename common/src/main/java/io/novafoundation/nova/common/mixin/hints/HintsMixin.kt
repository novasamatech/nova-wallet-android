package io.novafoundation.nova.common.mixin.hints

import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface HintsMixin {

    val hintsFlow: Flow<List<String>>
}


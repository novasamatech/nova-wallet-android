package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints

import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.coroutines.CoroutineScope

class ReferendumVoteHintsMixinFactory(
    private val resourcesHintsMixinFactory: ResourcesHintsMixinFactory
) {

    fun create(scope: CoroutineScope): HintsMixin {
        return resourcesHintsMixinFactory.create(
            coroutineScope = scope,
            hintsRes = listOf(R.string.referendum_vote_unlock_hint)
        )
    }
}

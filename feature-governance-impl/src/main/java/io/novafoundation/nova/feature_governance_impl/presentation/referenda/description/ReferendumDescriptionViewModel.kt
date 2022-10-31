package io.novafoundation.nova.feature_governance_impl.presentation.referenda.description

import io.noties.markwon.Markwon
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter

class ReferendumDescriptionViewModel(
    private val router: GovernanceRouter,
    private val payload: ReferendumDescriptionPayload,
    val markwon: Markwon,
) : BaseViewModel() {

    val referendumTitle = payload.title
    val markdownDescription = flowOf { markwon.toMarkdown(payload.description) }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }
}

package io.novafoundation.nova.feature_governance_impl.presentation.common.description

import io.noties.markwon.Markwon
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.description.DescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter

class DescriptionViewModel(
    private val router: GovernanceRouter,
    private val payload: DescriptionPayload,
    val markwon: Markwon,
) : BaseViewModel() {

    val title = payload.title
    val toolbarTitle = payload.toolbarTitle

    val markdownDescription = flowOf { markwon.toMarkdown(payload.description) }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }
}

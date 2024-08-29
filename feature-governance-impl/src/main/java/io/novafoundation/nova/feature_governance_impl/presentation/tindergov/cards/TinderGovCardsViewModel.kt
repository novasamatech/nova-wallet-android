package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter

class TinderGovCardsViewModel(
    private val router: GovernanceRouter
) : BaseViewModel() {

    fun back() {
        router.back()
    }

    fun ayeClicked() {
        showMessage("Not implemented yet")
    }

    fun abstainClicked() {
        showMessage("Not implemented yet")
    }

    fun nayClicked() {
        showMessage("Not implemented yet")
    }

    fun openReadMore(item: TinderGovCardRvItem) {
        showMessage("Not implemented yet")
    }
}

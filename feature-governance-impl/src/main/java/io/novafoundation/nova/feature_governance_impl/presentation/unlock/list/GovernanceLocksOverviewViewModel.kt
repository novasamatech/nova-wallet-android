package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigInteger

class GovernanceLocksOverviewViewModel(
    private val router: GovernanceRouter
) : BaseViewModel() {

    val totalAmount = flowOf {
        AmountModel("112.1 KSM", "$1,514.12")
    }.share()

    val lockModels = flowOf {
        listOf(
            GovernanceLockModel(
                ReferendumId(BigInteger.ONE),
                "10 KSM",
                "Unlockable",
                R.color.multicolor_green_100,
                null,
                null
            )
        ) + List(100) {
            GovernanceLockModel(
                ReferendumId(BigInteger.ONE),
                "2$it KSM",
                "1$it days left",
                R.color.white_64,
                R.drawable.ic_time_16,
                R.color.white_48
            )
        }
    }.share()

    fun backClicked() {
        router.back()
    }
}

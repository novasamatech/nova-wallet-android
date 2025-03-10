package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface FeeInspector<F> {

    class InspectedFeeAmount(
        /**
         * The amount that is checked against [Asset.balanceCountedTowardsEDInPlanks] by the node before transaction execution
         * This is always the submission fee amount
         */
        val checkedAgainstMinimumBalance: Balance,

        /**
         * The total amount that will either be directly paid from or required to be kept on [Asset.transferable]
         * For example, some fees might deduct only submission fee but still require additional balance to be kept on transferable
         */
        val deductedFromTransferable: Balance
    )

    fun inspectFeeAmount(fee: F): InspectedFeeAmount

    fun getSubmissionFeeAsset(fee: F): Chain.Asset
}

package io.novafoundation.nova.feature_assets.domain.send.model

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.model.getAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class TransferFee(
    val originFee: OriginFee,
    val crossChainFee: FeeBase?
) : MaxAvailableDeduction {

    fun totalFeeByExecutingAccount(chainAsset: Chain.Asset): Balance {
        val accountThatPaysFees = originFee.submissionFee.submissionOrigin.executingAccount

        val submission = originFee.submissionFee.getAmount(chainAsset, accountThatPaysFees)
        val delivery = originFee.deliveryFee?.getAmount(chainAsset, accountThatPaysFees).orZero()

        return submission + delivery
    }

    fun replaceSubmission(newSubmissionFee: SubmissionFee): TransferFee {
        return copy(originFee = originFee.copy(newSubmissionFee))
    }

    override fun maxAmountDeductionFor(amountAsset: Chain.Asset): Balance {
        // Delegate submission calculation to submission fee itself
        val submission = originFee.submissionFee.maxAmountDeductionFor(amountAsset)
        // Delivery is always paid from executing account
        val delivery = originFee.deliveryFee?.getAmount(amountAsset).orZero()
        // Execution is paid from the sending amount itself, so we subtract it as well since we later add it on top of sending amount
        val execution = crossChainFee?.getAmount(amountAsset).orZero()

        return submission + delivery + execution
    }
}

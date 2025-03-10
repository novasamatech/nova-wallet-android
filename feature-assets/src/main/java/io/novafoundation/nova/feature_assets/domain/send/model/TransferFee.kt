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
        return originFee.submissionFee.amount +
            originFee.deliveryFee?.amount.orZero() +
            crossChainFee?.getAmount(amountAsset).orZero()
    }
}

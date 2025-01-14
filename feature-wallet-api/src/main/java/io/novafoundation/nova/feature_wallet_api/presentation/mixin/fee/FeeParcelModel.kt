package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.EvmFee
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.runtime.util.ChainAssetParcel
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

sealed interface FeeParcelModel : Parcelable {

    val amount: BigInteger

    val submissionOrigin: SubmissionOriginParcelModel

    val asset: ChainAssetParcel
}

@Parcelize
class SubmissionOriginParcelModel(
    val executingAccount: AccountId,
    val signingAccount: AccountId
) : Parcelable

@Parcelize
class EvmFeeParcelModel(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
    override val amount: BigInteger,
    override val submissionOrigin: SubmissionOriginParcelModel,
    override val asset: ChainAssetParcel
) : FeeParcelModel

@Parcelize
class SimpleFeeParcelModel(
    override val amount: BigInteger,
    override val submissionOrigin: SubmissionOriginParcelModel,
    override val asset: ChainAssetParcel
) : FeeParcelModel

fun mapFeeToParcel(fee: Fee): FeeParcelModel {
    val submissionOrigin = mapSubmissionOriginToParcel(fee.submissionOrigin)
    val assetParcel = ChainAssetParcel(fee.asset)

    return when (fee) {
        is EvmFee -> EvmFeeParcelModel(
            gasLimit = fee.gasLimit,
            gasPrice = fee.gasPrice,
            amount = fee.amount,
            submissionOrigin = submissionOrigin,
            asset = assetParcel
        )

        else -> SimpleFeeParcelModel(
            amount = fee.amount,
            submissionOrigin = submissionOrigin,
            asset = assetParcel
        )
    }
}

private fun mapSubmissionOriginToParcel(submissionOrigin: SubmissionOrigin): SubmissionOriginParcelModel {
    return with(submissionOrigin) { SubmissionOriginParcelModel(executingAccount = executingAccount, signingAccount = signingAccount) }
}

fun mapFeeFromParcel(parcelFee: FeeParcelModel): Fee {
    val submissionOrigin = mapSubmissionOriginFromParcel(parcelFee.submissionOrigin)

    return when (parcelFee) {
        is EvmFeeParcelModel -> EvmFee(
            gasLimit = parcelFee.gasLimit,
            gasPrice = parcelFee.gasPrice,
            submissionOrigin,
            parcelFee.asset.value
        )

        is SimpleFeeParcelModel -> SubstrateFee(parcelFee.amount, submissionOrigin, parcelFee.asset.value)
    }
}

private fun mapSubmissionOriginFromParcel(submissionOrigin: SubmissionOriginParcelModel): SubmissionOrigin {
    return with(submissionOrigin) { SubmissionOrigin(executingAccount = executingAccount, signingAccount = signingAccount) }
}

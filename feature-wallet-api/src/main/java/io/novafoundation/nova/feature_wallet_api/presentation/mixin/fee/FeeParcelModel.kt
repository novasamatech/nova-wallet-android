package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.EvmFee
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import io.novafoundation.nova.runtime.util.FullAssetIdModel
import io.novafoundation.nova.runtime.util.toDomain
import io.novafoundation.nova.runtime.util.toModel
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

sealed interface FeeParcelModel : Parcelable {

    val amount: BigDecimal

    val submissionOrigin: SubmissionOriginParcelModel
}

@Parcelize
class SubmissionOriginParcelModel(
    val requested: AccountId,
    val actual: AccountId
) : Parcelable

@Parcelize
class EvmFeeParcelModel(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
    override val amount: BigDecimal,
    override val submissionOrigin: SubmissionOriginParcelModel
) : FeeParcelModel

@Parcelize
class SimpleFeeParcelModel(
    val planks: BigInteger,
    override val amount: BigDecimal,
    override val submissionOrigin: SubmissionOriginParcelModel,
    val asset: PaymentAsset
) : FeeParcelModel {

    sealed interface PaymentAsset : Parcelable {

        @Parcelize
        object Native : PaymentAsset

        @Parcelize
        class Asset(val assetId: FullAssetIdModel) : PaymentAsset
    }
}

fun mapFeeToParcel(decimalFee: GenericDecimalFee<*>): FeeParcelModel {
    val submissionOrigin = mapSubmissionOriginToParcel(decimalFee.networkFee.submissionOrigin)

    return when (val fee = decimalFee.networkFee) {
        is EvmFee -> EvmFeeParcelModel(gasLimit = fee.gasLimit, gasPrice = fee.gasPrice, amount = decimalFee.networkFeeDecimalAmount, submissionOrigin)
        else -> SimpleFeeParcelModel(
            decimalFee.networkFee.amount,
            decimalFee.networkFeeDecimalAmount,
            submissionOrigin,
            mapPaymentAssetToParcel(fee.paymentAsset)
        )
    }
}

private fun mapSubmissionOriginToParcel(submissionOrigin: SubmissionOrigin): SubmissionOriginParcelModel {
    return with(submissionOrigin) { SubmissionOriginParcelModel(requested = requestedOrigin, actual = actualOrigin) }
}

fun mapFeeFromParcel(parcelFee: FeeParcelModel): DecimalFee {
    val submissionOrigin = mapSubmissionOriginFromParcel(parcelFee.submissionOrigin)

    val fee = when (parcelFee) {
        is EvmFeeParcelModel -> EvmFee(
            gasLimit = parcelFee.gasLimit,
            gasPrice = parcelFee.gasPrice,
            submissionOrigin,
            Fee.PaymentAsset.Native
        )

        is SimpleFeeParcelModel -> SubstrateFee(parcelFee.planks, submissionOrigin, mapPaymentAssetFromParcel(parcelFee.asset))
    }

    return DecimalFee(SimpleFee(fee), parcelFee.amount)
}

fun mapPaymentAssetToParcel(paymentAsset: Fee.PaymentAsset): SimpleFeeParcelModel.PaymentAsset {
    return when (paymentAsset) {
        Fee.PaymentAsset.Native -> SimpleFeeParcelModel.PaymentAsset.Native
        is Fee.PaymentAsset.Asset -> SimpleFeeParcelModel.PaymentAsset.Asset(paymentAsset.assetId.toModel())
    }
}

fun mapPaymentAssetFromParcel(paymentAsset: SimpleFeeParcelModel.PaymentAsset): Fee.PaymentAsset {
    return when (paymentAsset) {
        SimpleFeeParcelModel.PaymentAsset.Native -> Fee.PaymentAsset.Native
        is SimpleFeeParcelModel.PaymentAsset.Asset -> Fee.PaymentAsset.Asset(paymentAsset.assetId.toDomain())
    }
}

private fun mapSubmissionOriginFromParcel(submissionOrigin: SubmissionOriginParcelModel): SubmissionOrigin {
    return with(submissionOrigin) { SubmissionOrigin(requestedOrigin = requested, actualOrigin = actual) }
}

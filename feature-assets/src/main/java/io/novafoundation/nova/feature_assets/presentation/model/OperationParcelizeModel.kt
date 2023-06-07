package io.novafoundation.nova.feature_assets.presentation.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

sealed class OperationParcelizeModel : Parcelable {

    @Parcelize
    class Reward(
        val chainId: ChainId,
        val eventId: String,
        val address: String,
        val time: Long,
        val amount: String,
        val fiatAmount: String?,
        val type: String,
        val era: String,
        val validator: String?,
        val statusAppearance: OperationStatusAppearance,
    ) : OperationParcelizeModel()

    @Parcelize
    class Extrinsic(
        val chainId: ChainId,
        val chainAssetId: ChainAssetId,
        val time: Long,
        val originAddress: String,
        val content: ExtrinsicContentParcel,
        val fee: String,
        val fiatFee: String?,
        val statusAppearance: OperationStatusAppearance,
    ) : Parcelable, OperationParcelizeModel()

    @Parcelize
    class Transfer(
        val chainId: ChainId,
        val assetId: Int,
        val time: Long,
        val address: String,
        val hash: String?,
        val isIncome: Boolean,
        val formattedAmount: String,
        val formattedFiatAmount: String?,
        val receiver: String,
        val sender: String,
        val fee: BigInteger?,
        val formattedFee: String,
        val statusAppearance: OperationStatusAppearance,
        @DrawableRes val transferDirectionIcon: Int
    ) : Parcelable, OperationParcelizeModel()
}

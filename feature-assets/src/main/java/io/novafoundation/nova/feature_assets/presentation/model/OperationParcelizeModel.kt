package io.novafoundation.nova.feature_assets.presentation.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

sealed class OperationParcelizeModel : Parcelable {

    @Parcelize
    class Reward(
        val chainId: ChainId,
        val eventId: String,
        val address: String,
        val time: Long,
        val amount: AmountParcelModel,
        val type: String,
        val era: String?,
        val validator: String?,
        val statusAppearance: OperationStatusAppearance,
    ) : OperationParcelizeModel()

    @Parcelize
    class PoolReward(
        val chainId: ChainId,
        val address: String,
        val time: Long,
        val amount: AmountParcelModel,
        val type: String,
        val poolId: Int,
        val eventId: String,
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
        val amount: AmountParcelModel,
        val receiver: String,
        val sender: String,
        val fee: BigInteger?,
        val statusAppearance: OperationStatusAppearance,
        @DrawableRes val transferDirectionIcon: Int
    ) : Parcelable, OperationParcelizeModel()

    @Parcelize
    class Swap(
        val amountIsAssetIn: Boolean,
        val timeMillis: Long,
        val amountIn: ChainAssetWithAmountParcelModel,
        val amountOut: ChainAssetWithAmountParcelModel,
        val amountFee: ChainAssetWithAmountParcelModel,
        val originAddress: String,
        val transactionHash: String?,
        val statusAppearance: OperationStatusAppearance,
    ) : Parcelable, OperationParcelizeModel()
}

@Parcelize
class ChainAssetWithAmountParcelModel(
    val assetId: AssetPayload,
    val amount: Balance
) : Parcelable

@Parcelize
class AmountParcelModel(
    val token: String,
    val fiat: String?
) : Parcelable

fun AmountParcelModel.toAmountModel(): AmountModel {
    return AmountModel(token, fiat)
}

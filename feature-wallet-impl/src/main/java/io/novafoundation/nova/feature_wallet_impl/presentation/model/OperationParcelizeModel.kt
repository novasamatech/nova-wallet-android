package io.novafoundation.nova.feature_wallet_impl.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

sealed class OperationParcelizeModel : Parcelable {

    @Parcelize
    class Reward(
        val chainId: ChainId,
        val eventId: String,
        val address: String,
        val time: Long,
        val amount: String,
        val isReward: Boolean,
        val era: Int,
        val validator: String?,
    ) : OperationParcelizeModel()

    @Parcelize
    class Extrinsic(
        val chainId: ChainId,
        val time: Long,
        val originAddress: String,
        val hash: String,
        val module: String,
        val call: String,
        val fee: String,
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
        val amount: String,
        val total: String,
        val receiver: String,
        val sender: String,
        val fee: String,
        val statusAppearance: OperationStatusAppearance,
    ) : Parcelable, OperationParcelizeModel()
}

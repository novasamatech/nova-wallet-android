package io.novafoundation.nova.feature_swap_api.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.android.parcel.Parcelize
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

sealed interface SwapSettingsPayload : Parcelable {

    val assetIn: AssetPayload

    @Parcelize
    class DefaultFlow(override val assetIn: AssetPayload) : SwapSettingsPayload

    @Parcelize
    class RepeatOperation(
        override val assetIn: AssetPayload,
        val assetOut: AssetPayload,
        val feeAsset: AssetPayload,
        val amount: Balance,
        val direction: SwapDirectionModel
    ) : SwapSettingsPayload
}

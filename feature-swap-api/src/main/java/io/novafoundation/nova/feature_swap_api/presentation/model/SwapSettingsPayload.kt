package io.novafoundation.nova.feature_swap_api.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.parcelize.Parcelize
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

sealed interface SwapSettingsPayload : Parcelable {

    val assetIn: AssetPayload?

    /** Where the user came from, so analytics can tell the entry points apart. */
    val source: SwapEntryPoint

    @Parcelize
    data object Restore : SwapSettingsPayload {

        override val assetIn: AssetPayload? = null

        override val source: SwapEntryPoint = SwapEntryPoint.MAIN_SCREEN
    }

    @Parcelize
    class Prefilled(
        override val assetIn: AssetPayload?,
        val assetOut: AssetPayload?,
        override val source: SwapEntryPoint
    ) : SwapSettingsPayload

    @Parcelize
    class DefaultFlow(
        override val assetIn: AssetPayload,
        override val source: SwapEntryPoint
    ) : SwapSettingsPayload

    @Parcelize
    class RepeatOperation(
        override val assetIn: AssetPayload,
        val assetOut: AssetPayload,
        val amount: Balance,
        val direction: SwapDirectionParcel,
        override val source: SwapEntryPoint,
        val metaAccountId: Long? = null,
        val slippage: Double? = null
    ) : SwapSettingsPayload
}

enum class SwapEntryPoint {
    ASSET_DETAILS,
    MAIN_SCREEN,
    OPERATION_DETAILS,
    RETRY
}

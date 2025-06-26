package io.novafoundation.nova.feature_swap_impl.presentation.main.view

import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textWithDescriptionItem
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.model.GetAssetInOption
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class GetAssetInBottomSheet(
    context: Context,
    onCancel: () -> Unit,
    private val payload: Payload,
    private val onClicked: (GetAssetInOption) -> Unit,
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(
    context,
    viewConfiguration = ViewConfiguration.default(context),
    onCancel = onCancel
) {

    class Payload(
        val chainAsset: Chain.Asset,
        val availableOptions: Set<GetAssetInOption>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(formatWithAssetSymbol(R.string.swap_get_token_using))

        getAssetInItem(
            title = context.getString(R.string.wallet_cross_chain_transfer),
            description = formatWithAssetSymbol(R.string.swap_get_token_cross_chain_description),
            iconRes = R.drawable.ic_cross_chain,
            option = GetAssetInOption.CROSS_CHAIN
        )

        getAssetInItem(
            title = context.getString(R.string.wallet_asset_receive),
            description = formatWithAssetSymbol(R.string.swap_get_token_receive_description),
            iconRes = R.drawable.ic_arrow_down,
            option = GetAssetInOption.RECEIVE
        )

        getAssetInItem(
            title = context.getString(R.string.wallet_asset_buy),
            description = formatWithAssetSymbol(R.string.swap_get_token_buy_description),
            iconRes = R.drawable.ic_buy,
            option = GetAssetInOption.BUY
        )
    }

    private fun getAssetInItem(
        title: String,
        description: String,
        @DrawableRes iconRes: Int,
        option: GetAssetInOption
    ) {
        textWithDescriptionItem(
            title = title,
            description = description,
            iconRes = iconRes,
            enabled = option in payload.availableOptions,
            showArrowWhenEnabled = true
        ) {
            onClicked(option)
        }
    }

    private fun formatWithAssetSymbol(@StringRes resId: Int) = context.getString(resId, payload.chainAsset.symbol)
}

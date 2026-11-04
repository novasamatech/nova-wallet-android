package io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textWithDescriptionItem
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.ItemGetAssetDappBinding
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class GetAssetBottomSheet(
    context: Context,
    private val onCancel: () -> Unit,
    private val payload: Payload,
    private val onClicked: (GetAssetOption) -> Unit,
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(
    context,
    viewConfiguration = ViewConfiguration.default(context)
) {

    class Payload(
        val chainAsset: Chain.Asset,
        val availableOptions: Set<GetAssetOption>,
        val optionUpdates: Flow<Set<GetAssetOption>>? = null
    )

    private val headerViewCount = binder.fixedListSheetItemContainer.childCount

    private var availableOptions = payload.availableOptions

    private var actionCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(formatWithAssetSymbol(R.string.swap_get_token_using))
        renderOptions()
        payload.optionUpdates
            ?.onEach { options ->
                if (options != availableOptions) {
                    availableOptions = options
                    renderOptions()
                }
            }
            ?.launchIn(this)
    }

    override fun dismiss() {
        super.dismiss()
        if (!actionCompleted) {
            actionCompleted = true
            onCancel()
        }
    }

    private fun selectOption(option: GetAssetOption) {
        if (actionCompleted) return
        actionCompleted = true
        onClicked(option)
    }

    private fun renderOptions() {
        val container = binder.fixedListSheetItemContainer
        container.removeViews(headerViewCount, container.childCount - headerViewCount)

        getAssetInItem(
            title = context.getString(R.string.wallet_cross_chain_transfer),
            description = formatWithAssetSymbol(R.string.swap_get_token_cross_chain_description),
            iconRes = R.drawable.ic_cross_chain,
            option = GetAssetOption.CrossChain
        )

        getAssetInItem(
            title = context.getString(R.string.wallet_asset_receive),
            description = formatWithAssetSymbol(R.string.swap_get_token_receive_description),
            iconRes = R.drawable.ic_arrow_down,
            option = GetAssetOption.Receive
        )

        getAssetInItem(
            title = context.getString(R.string.wallet_asset_buy),
            description = formatWithAssetSymbol(R.string.swap_get_token_buy_description),
            iconRes = R.drawable.ic_buy,
            option = GetAssetOption.Buy
        )

        availableOptions.filterIsInstance<GetAssetOption.DApp>()
            .forEach(::dAppItem)
    }

    private fun dAppItem(option: GetAssetOption.DApp) {
        val itemBinding = ItemGetAssetDappBinding.inflate(LayoutInflater.from(context))

        item(itemBinding) { itemBinder ->
            itemBinder.dappTitle.text = context.getString(R.string.swap_get_token_dapp_title, option.name)
            itemBinder.dappSubtitle.text = dAppDescription(option.supportedTokens)

            itemBinder.root.setDismissingClickListener {
                selectOption(option)
            }
        }
    }

    private fun dAppDescription(supportedTokens: List<TokenSymbol>): String {
        if (supportedTokens.isEmpty()) {
            return context.getString(R.string.swap_get_token_dapp_browser_description)
        }

        val tokenOptions = when (supportedTokens.size) {
            1 -> supportedTokens.single().value
            else -> context.getString(
                R.string.swap_get_token_alternatives_format,
                supportedTokens.dropLast(1).joinToString(", ") { it.value },
                supportedTokens.last().value
            )
        }

        return context.getString(R.string.swap_get_token_dapp_description, tokenOptions)
    }

    private fun getAssetInItem(
        title: String,
        description: String,
        @DrawableRes iconRes: Int,
        option: GetAssetOption
    ) {
        textWithDescriptionItem(
            title = title,
            description = description,
            iconRes = iconRes,
            enabled = option in availableOptions,
            showArrowWhenEnabled = true
        ) {
            selectOption(option)
        }
    }

    private fun formatWithAssetSymbol(@StringRes resId: Int) = context.getString(resId, payload.chainAsset.symbol)
}

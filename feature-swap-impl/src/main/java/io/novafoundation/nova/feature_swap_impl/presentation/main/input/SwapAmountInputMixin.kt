package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase
import kotlinx.coroutines.flow.Flow

interface SwapAmountInputMixin : AmountChooserMixinBase {

    val assetModel: Flow<SwapInputAssetModel>

    interface Presentation : SwapAmountInputMixin, AmountChooserMixinBase.Presentation

    class SwapInputAssetModel(
        val assetIcon: SwapAssetIcon,
        val title: String,
        val subtitleIcon: Icon?,
        val subtitle: String,
        val showInput: Boolean,
    ) {
        sealed class SwapAssetIcon {

            class Chosen(val assetIcon: Icon) : SwapAssetIcon()

            object NotChosen : SwapAssetIcon()
        }
    }
}

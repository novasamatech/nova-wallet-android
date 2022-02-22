package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.AmountView
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetSelectorModel

interface WithAmountChooser {

    val amountChooserMixin: AmountChooserMixin
}

fun BaseFragment<*>.setupAmountChooser(
    withChooser: WithAmountChooser,
    amountView: AmountView,
) {
    amountView.amountInput.bindTo(withChooser.amountChooserMixin.amountInput, lifecycleScope)

    withChooser.amountChooserMixin.assetModel.observe(amountView::setAssetModel)

    withChooser.amountChooserMixin.fiatAmount.observe(amountView::setFiatAmount)
}

fun AmountView.setAssetModel(assetModel: AssetSelectorModel) {
    setAssetBalance(assetModel.assetBalance)
    setAssetName(assetModel.tokenName)
    loadAssetImage(assetModel.imageUrl)
}

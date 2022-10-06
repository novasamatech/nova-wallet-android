package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.WithAssetSelector

class ReferendaListViewModel(
    assetSelectorFactory: MixinFactory<AssetSelectorMixin.Presentation>,
) : BaseViewModel(), WithAssetSelector {

    override val assetSelectorMixin = assetSelectorFactory.create(scope = this)
}

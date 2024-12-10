package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChooseFeeCurrencyPayload(val selectedCommissionAsset: Chain.Asset, val availableAssets: List<Chain.Asset>)

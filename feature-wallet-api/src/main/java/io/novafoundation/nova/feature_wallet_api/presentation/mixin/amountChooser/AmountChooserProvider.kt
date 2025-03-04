package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AmountChooserProviderFactory(
    private val assetIconProvider: AssetIconProvider,
) : AmountChooserMixin.Factory {

    override fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        maxActionProvider: MaxActionProvider?
    ): AmountChooserProvider {
        return AmountChooserProvider(
            coroutineScope = scope,
            usedAssetFlow = assetFlow,
            assetIconProvider = assetIconProvider,
            maxActionProvider = maxActionProvider
        )
    }
}

class AmountChooserProvider(
    coroutineScope: CoroutineScope,
    override val usedAssetFlow: Flow<Asset>,
    private val assetIconProvider: AssetIconProvider,
    maxActionProvider: MaxActionProvider?
) : BaseAmountChooserProvider(
    coroutineScope = coroutineScope,
    tokenFlow = usedAssetFlow.map { it.token },
    maxActionProvider = maxActionProvider,
),
    AmountChooserMixin.Presentation {

    override val assetModel = usedAssetFlow.map { asset ->
        ChooseAmountModel(asset, assetIconProvider)
    }
        .shareInBackground()
}

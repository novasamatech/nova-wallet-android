package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.validation.FieldValidator
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
        maxActionProvider: MaxActionProvider?,
        fieldValidator: FieldValidator?
    ): AmountChooserProvider {
        return AmountChooserProvider(
            coroutineScope = scope,
            usedAssetFlow = assetFlow,
            assetIconProvider = assetIconProvider,
            maxActionProvider = maxActionProvider,
            fieldValidator = fieldValidator
        )
    }
}

class AmountChooserProvider(
    coroutineScope: CoroutineScope,
    override val usedAssetFlow: Flow<Asset>,
    private val assetIconProvider: AssetIconProvider,
    maxActionProvider: MaxActionProvider?,
    fieldValidator: FieldValidator?
) : BaseAmountChooserProvider(
    coroutineScope = coroutineScope,
    tokenFlow = usedAssetFlow.map { it.token },
    maxActionProvider = maxActionProvider,
    fieldValidator = fieldValidator
),
    AmountChooserMixin.Presentation {

    override val assetModel = usedAssetFlow.map { asset ->
        ChooseAmountModel(asset, assetIconProvider)
    }
        .shareInBackground()
}

package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.annotation.StringRes
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class AmountChooserProviderFactory(
    private val resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
) : AmountChooserMixin.Factory {

    override fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        balanceLabel: Int?,
        maxActionProvider: MaxActionProvider?
    ): AmountChooserProvider {
        return AmountChooserProvider(
            coroutineScope = scope,
            usedAssetFlow = assetFlow,
            balanceLabel = balanceLabel,
            resourceManager = resourceManager,
            assetIconProvider = assetIconProvider,
            maxActionProvider = maxActionProvider
        )
    }

    override fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        balanceField: (Asset) -> BigDecimal,
        @StringRes balanceLabel: Int?,
        maxActionProvider: MaxActionProvider?
    ): AmountChooserMixin.Presentation {
        return create(
            scope = scope,
            assetFlow = assetFlow,
            balanceLabel = balanceLabel,
            maxActionProvider = maxActionProvider
        )
    }
}

class AmountChooserProvider(
    coroutineScope: CoroutineScope,
    override val usedAssetFlow: Flow<Asset>,
    private val resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    @StringRes private val balanceLabel: Int?,
    maxActionProvider: MaxActionProvider?
) : BaseAmountChooserProvider(
    coroutineScope = coroutineScope,
    tokenFlow = usedAssetFlow.map { it.token },
    maxActionProvider = maxActionProvider,
),
    AmountChooserMixin.Presentation {

    override val assetModel = usedAssetFlow.map { asset ->
        ChooseAmountModel(asset, assetIconProvider, resourceManager, balanceLabel)
    }
        .shareInBackground()
}

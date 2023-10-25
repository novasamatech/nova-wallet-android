package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.SimpleMaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.BigInteger

class AmountChooserProviderFactory(
    private val resourceManager: ResourceManager
) : AmountChooserMixin.Factory {

    override fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        availableBalanceFlow: Flow<BigInteger>,
        balanceLabel: Int?
    ): AmountChooserProvider {
        return AmountChooserProvider(
            coroutineScope = scope,
            usedAssetFlow = assetFlow,
            balanceLabel = balanceLabel,
            resourceManager = resourceManager,
            maxActionProvider = SimpleMaxActionProvider(
                maxAvailableForDisplay = availableBalanceFlow,
                // TODO amount chooser max button
                maxAvailableForAction = flowOf(null)
            )
        )
    }

    override fun create(
        scope: CoroutineScope,
        assetFlow: Flow<Asset>,
        balanceField: (Asset) -> BigDecimal,
        @StringRes balanceLabel: Int?
    ): AmountChooserMixin.Presentation {
        return create(
            scope = scope,
            assetFlow = assetFlow,
            availableBalanceFlow = assetFlow.map { it.token.planksFromAmount(balanceField(it)) },
            balanceLabel = balanceLabel,
        )
    }
}

class AmountChooserProvider(
    coroutineScope: CoroutineScope,
    override val usedAssetFlow: Flow<Asset>,
    private val resourceManager: ResourceManager,
    @StringRes private val balanceLabel: Int?,
    maxActionProvider: MaxActionProvider
) : BaseAmountChooserProvider(
    coroutineScope = coroutineScope,
    tokenFlow = usedAssetFlow.map { it.token },
    maxActionProvider = maxActionProvider,
),
    AmountChooserMixin.Presentation {

    override val assetModel = usedAssetFlow.map { asset ->
        ChooseAmountModel(asset, resourceManager, balanceLabel)
    }
        .shareInBackground()
}

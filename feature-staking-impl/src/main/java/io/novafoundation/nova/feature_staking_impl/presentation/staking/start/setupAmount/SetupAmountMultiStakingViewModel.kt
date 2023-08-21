package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.currentSelectionFlow
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType.MultiStakingSelectionTypeProviderFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.toStakingOptionIds
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.model.StakingPropertiesModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class SetupAmountMultiStakingViewModel(
    private val multiStakingSelectionFormatter: MultiStakingSelectionFormatter,
    private val resourceManager: ResourceManager,
    private val router: StartMultiStakingRouter,
    multiStakingSelectionTypeProviderFactory: MultiStakingSelectionTypeProviderFactory,
    assetUseCase: ArbitraryAssetUseCase,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    selectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    payload: SetupAmountMultiStakingPayload
) : BaseViewModel() {
    private val multiStakingSelectionTypeProvider = multiStakingSelectionTypeProviderFactory.create(
        scope = viewModelScope,
        candidateOptionsIds = payload.availableStakingOptions.toStakingOptionIds()
    )

    private val multiStakingSelectionTypeFlow = multiStakingSelectionTypeProvider.multiStakingSelectionTypeFlow()
        .shareInBackground()

    private val currentSelectionFlow = selectionStoreProvider.currentSelectionFlow(viewModelScope)
        .shareInBackground()

    val currentAssetFlow = assetUseCase.assetFlow(
        chainId = payload.availableStakingOptions.chainId,
        assetId = payload.availableStakingOptions.assetId
    ).shareInBackground()

    val availableBalance = combine(
        currentAssetFlow,
        multiStakingSelectionTypeFlow
    ) { currentAsset, multiStakingSelectionType ->
        multiStakingSelectionType.availableBalance(currentAsset)
    }.shareInBackground()

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = viewModelScope,
        assetFlow = currentAssetFlow,
        availableBalanceFlow = availableBalance,
        balanceLabel = R.string.wallet_balance_available
    )

    private val amountEmptyFlow = amountChooserMixin.amountInput
        .map { it.isEmpty() }
        .distinctUntilChanged()

    val stakingPropertiesModel = combine(
        amountEmptyFlow,
        currentSelectionFlow
    ) { amountEmpty, currentSelection ->
        when {
            currentSelection == null && amountEmpty -> StakingPropertiesModel.Hidden
            currentSelection == null -> StakingPropertiesModel.Loading
            else -> {
                val content = StakingPropertiesModel.Content(
                    estimatedReward = currentSelection.selection.apy.format(),
                    selection = multiStakingSelectionFormatter.formatForSetupAmount(currentSelection)
                )

                StakingPropertiesModel.Loaded(content)
            }
        }
    }.shareInBackground()

    val title = currentAssetFlow.map {
        val tokenSymbol = it.token.configuration.symbol

        resourceManager.getString(R.string.staking_stake_format, tokenSymbol)
    }.shareInBackground()

    init {
        combine(
            multiStakingSelectionTypeFlow,
            amountChooserMixin.amountInput
        ) { multiStakingSelectionType, amountInput ->
            val amount = amountInput.toBigDecimalOrNull() ?: return@combine
            val asset = currentAssetFlow.first()
            val planks = asset.token.planksFromAmount(amount)

            multiStakingSelectionType.updateSelectionFor(planks)
        }
            .inBackground()
            .launchIn(viewModelScope)
    }

    fun back() {
        router.back()
    }
}

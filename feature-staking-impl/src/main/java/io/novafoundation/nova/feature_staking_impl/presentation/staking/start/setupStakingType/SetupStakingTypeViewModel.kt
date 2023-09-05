package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.currentSelectionFlow
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.CompoundStakingTypeDetailsProvidersFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.ValidatedStakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.SelectionTypeSource
import java.math.BigInteger
import kotlinx.coroutines.launch

class SetupStakingTypeViewModel(
    private val router: StakingRouter,
    private val assetUseCase: ArbitraryAssetUseCase,
    payload: SetupStakingTypePayload,
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableStakingTypeItemFormatter: EditableStakingTypeItemFormatter,
    private val compoundStakingTypeDetailsProvidersFactory: CompoundStakingTypeDetailsProvidersFactory,
    private val validationExecutor: ValidationExecutor,
    chainRegistry: ChainRegistry
) : BaseViewModel(), Validatable by validationExecutor {

    private val chainWithAsset = flowOf {
        chainRegistry.chainWithAsset(
            payload.availableStakingOptions.chainId,
            payload.availableStakingOptions.assetId
        )
    }

    private val assetFlow = assetUseCase.assetFlow(
        payload.availableStakingOptions.chainId,
        payload.availableStakingOptions.assetId
    )

    private val stakingTypeDetailsProvidersFlow = chainWithAsset.map {
        compoundStakingTypeDetailsProvidersFactory.create(
            viewModelScope,
            it,
            payload.availableStakingOptions.stakingTypes
        )
    }

    private val editableSelectionFlow = editableSelectionStoreProvider.currentSelectionFlow(viewModelScope)
        .filterNotNull()
        .shareInBackground()

    private val currentSelectionFlow = currentSelectionStoreProvider.currentSelectionFlow(viewModelScope)
        .filterNotNull()
        .shareInBackground()

    private val editableStakingTypeComparator = getEditableStakingTypeComparator()

    private val stakingTypesDataFlow = stakingTypeDetailsProvidersFlow
        .flatMapLatest { it.getStakingTypeDetails() }
        .map { it.sortedWith(editableStakingTypeComparator) }
        .shareInBackground()

    val availableToRewriteData = combine(
        currentSelectionFlow,
        editableSelectionFlow
    ) { current, editable ->
        !current.selection.isSettingsEquals(editable.selection)
    }.shareInBackground()

    val stakingTypeModels = combine(
        assetFlow,
        stakingTypesDataFlow,
        editableSelectionFlow
    ) { asset, stakingTypesData, selection ->
        mapStakingTypes(asset, stakingTypesData, selection)
    }
        .shareInBackground()

    init {
        currentSelectionFlow
            .onEach {
                editableSelectionStoreProvider
                    .getSelectionStore(viewModelScope)
                    .updateSelection(it)
            }.launchIn(this)
    }

    fun backPressed() {
        // TODO: request access to close

        router.back()
    }

    fun donePressed() {
        launch {
            //TODO use SetupStakingTypeSelectionMixin.apply() after merging

            router.back()
        }
    }

    fun selectStakingType(stakingTypeRVItem: EditableStakingTypeRVItem, position: Int) {
        if (stakingTypeRVItem.isSelected) return

        launch {
            val chainAsset = chainWithAsset.first().asset
            val validatedStakingType = stakingTypesDataFlow.first()[position]
            val validationStatus = validatedStakingType.validationStatus ?: return@launch
            when (validationStatus) {
                is ValidationStatus.Valid -> {
                    setRecommendedSelection(validatedStakingType.stakingTypeDetails.stakingType)
                }

                is ValidationStatus.NotValid -> {
                    // provide error dialog
                    // handleSetupStakingTypeValidationFailure(chainAsset, validationStatus.reason, resourceManager)
                }
            }
        }
    }

    private suspend fun setRecommendedSelection(stakingType: Chain.Asset.StakingType) {
        val currentStake = getEnteredAmount() ?: return

        val recommendedSelection = stakingTypeDetailsProvidersFlow.first().getRecommendationProvider(stakingType)
            .recommendedSelection(currentStake)

        val recommendableMultiStakingSelection = RecommendableMultiStakingSelection(
            source = SelectionTypeSource.Manual(contentRecommended = true),
            selection = recommendedSelection
        )

        editableSelectionStoreProvider.getSelectionStore(viewModelScope)
            .updateSelection(recommendableMultiStakingSelection)
    }

    private suspend fun mapStakingTypes(
        asset: Asset,
        stakingTypesDetails: List<ValidatedStakingTypeDetails>,
        selection: RecommendableMultiStakingSelection
    ): List<EditableStakingTypeRVItem> {
        return stakingTypesDetails.mapNotNull {
            editableStakingTypeItemFormatter.format(asset, it, selection)
        }
    }

    private suspend fun getEnteredAmount(): BigInteger? {
        return currentSelectionStoreProvider.getSelectionStore(viewModelScope)
            .currentSelection
            ?.selection
            ?.stake
    }
}

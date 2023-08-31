package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.getValidatorsOrEmpty
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.EditingStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.ValidatedStakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.SelectionMethod
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.isDirectStaking
import io.novafoundation.nova.runtime.ext.isPoolStaking
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SetupStakingTypeViewModel(
    private val router: StakingRouter,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val resourceManager: ResourceManager,
    payload: SetupStakingTypePayload,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editingStakingTypeSelectionMixinFactory: EditingStakingTypeSelectionMixinFactory,
    private val editableStakingTypeItemFormatter: EditableStakingTypeItemFormatter,
    private val validationExecutor: ValidationExecutor,
    private val setupStakingSharedState: SetupStakingSharedState,
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

    private val editingStakingTypeSelectionMixin = chainWithAsset
        .map {
            editingStakingTypeSelectionMixinFactory.create(
                viewModelScope,
                chainWithAsset = it,
                availableStakingTypes = payload.availableStakingOptions.stakingTypes
            )
        }
        .shareInBackground()

    private val currentSelectionFlow = editingStakingTypeSelectionMixin
        .flatMapLatest { it.currentSelectionFlow }
        .shareInBackground()

    private val stakingTypesDataFlow = editingStakingTypeSelectionMixin
        .flatMapLatest { it.getEditableStakingTypes() }
        .shareInBackground()

    private val editableSelection = editingStakingTypeSelectionMixin.flatMapLatest { it.editableSelectionFlow }
        .shareInBackground()

    val availableToRewriteData = editingStakingTypeSelectionMixin
        .flatMapLatest { it.availableToRewriteData }
        .shareInBackground()

    val stakingTypeModels = combine(
        assetFlow,
        stakingTypesDataFlow,
        editableSelection
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
            editingStakingTypeSelectionMixin.first().apply()

            router.back()
        }
    }

    fun stakingTypeClicked(stakingTypeRVItem: EditableStakingTypeRVItem, position: Int) {
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

    fun stakingTargetClicked(position: Int) {
        launch {
            val stakingType = stakingTypesDataFlow.first()[position]
                .stakingTypeDetails
                .stakingType

            val selectionStore = editableSelectionStoreProvider.getSelectionStore(viewModelScope)

            when {
                stakingType.isDirectStaking() -> {
                    setupStakingSharedState.set(SetupStakingProcess.ReadyToSubmit(selectionStore.getValidatorsOrEmpty(), SelectionMethod.CUSTOM))
                    router.openSelectCustomValidators()
                }
                stakingType.isPoolStaking() -> {
                    // TODO
                }
            }
        }
    }

    private fun setRecommendedSelection(stakingType: Chain.Asset.StakingType) {
        launch {
            editingStakingTypeSelectionMixin.first().setRecommendedSelection(stakingType)
        }
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
}

package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
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
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import java.math.BigInteger
import kotlinx.coroutines.launch

class SetupStakingTypeViewModel(
    private val router: StakingRouter,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val payload: SetupStakingTypePayload,
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableStakingTypeItemFormatter: EditableStakingTypeItemFormatter,
    private val compoundStakingTypeDetailsProvidersFactory: CompoundStakingTypeDetailsProvidersFactory,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val setupStakingTypeFlowExecutorFactory: SetupStakingTypeFlowExecutorFactory,
    private val setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    chainRegistry: ChainRegistry
) : BaseViewModel(), Validatable by validationExecutor {

    val closeConfirmationAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    private val setupStakingTypeSelectionMixin = setupStakingTypeSelectionMixinFactory.create(viewModelScope)

    private val chainWithAssetFlow = flowOf {
        chainRegistry.chainWithAsset(
            payload.availableStakingOptions.chainId,
            payload.availableStakingOptions.assetId
        )
    }

    private val assetFlow = assetUseCase.assetFlow(
        payload.availableStakingOptions.chainId,
        payload.availableStakingOptions.assetId
    )

    private val compoundStakingTypeDetailsProviderFlow = chainWithAssetFlow.map {
        compoundStakingTypeDetailsProvidersFactory.create(
            computationalScope = this,
            chainWithAsset = it,
            availableStakingTypes = payload.availableStakingOptions.stakingTypes
        )
    }

    private val editableSelectionFlow = editableSelectionStoreProvider.currentSelectionFlow(viewModelScope)
        .filterNotNull()
        .shareInBackground()

    private val currentSelectionFlow = currentSelectionStoreProvider.currentSelectionFlow(viewModelScope)
        .filterNotNull()
        .shareInBackground()

    private val editableStakingTypeComparator = getEditableStakingTypeComparator()

    private val stakingTypesDataFlow = compoundStakingTypeDetailsProviderFlow
        .flatMapLatest { it.getStakingTypeDetails() }
        .map { it.sortedWith(editableStakingTypeComparator) }
        .shareInBackground()

    val dataHasBeenChanged = combine(
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
        launch {
            val dataHasBeenChanged = dataHasBeenChanged.first()

            if (dataHasBeenChanged) {
                closeConfirmationAction.awaitAction(
                    ConfirmationDialogInfo(
                        R.string.common_confirmation_title,
                        R.string.common_close_confirmation_message,
                        R.string.common_close,
                        R.string.common_cancel,
                    )
                )
            }

            router.back()
        }
    }

    fun donePressed() {
        launch {
            setupStakingTypeSelectionMixin.apply()

            router.back()
        }
    }

    fun stakingTypeClicked(stakingTypeRVItem: EditableStakingTypeRVItem, position: Int) {
        if (stakingTypeRVItem.isSelected) return

        launch {
            val enteredAmount = getEnteredAmount() ?: return@launch
            val chainAsset = chainWithAssetFlow.first().asset
            val stakingType = stakingTypesDataFlow.first()[position]
                .stakingTypeDetails
                .stakingType

            val compoundStakingTypeDetailsProvider = compoundStakingTypeDetailsProviderFlow.first()
            val validationSystem = compoundStakingTypeDetailsProvider.getValidationSystem(stakingType)
            val payload = compoundStakingTypeDetailsProvider.getValidationPayload(stakingType) ?: return@launch

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { handleSetupStakingTypeValidationFailure(chainAsset, it, resourceManager) },
            ) {
                setRecommendedSelection(enteredAmount, stakingType)
            }
        }
    }

    fun stakingTargetClicked(position: Int) {
        launch {
            val stakingType = stakingTypesDataFlow.first()[position]
                .stakingTypeDetails
                .stakingType
            val setupStakingTypeFlowExecutor = setupStakingTypeFlowExecutorFactory.create(
                payload.availableStakingOptions.chainId,
                payload.availableStakingOptions.assetId,
                stakingType
            )
            setupStakingTypeFlowExecutor.execute(viewModelScope)
        }
    }

    private fun setRecommendedSelection(enteredAmount: BigInteger, stakingType: Chain.Asset.StakingType) {
        launch {
            val chainWithAsset = chainWithAssetFlow.first()
            val stakingOption = createStakingOption(chainWithAsset, stakingType)
            setupStakingTypeSelectionMixin.selectRecommended(viewModelScope, stakingOption, enteredAmount)
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

    private suspend fun getEnteredAmount(): BigInteger? {
        return currentSelectionStoreProvider.getSelectionStore(viewModelScope)
            .getCurrentSelection()
            ?.selection
            ?.stake
    }
}

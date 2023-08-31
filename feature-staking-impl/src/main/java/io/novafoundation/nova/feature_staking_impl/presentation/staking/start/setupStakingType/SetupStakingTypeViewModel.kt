package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.getValidatorsOrEmpty
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.EditingStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypePayload
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.EditableStakingType
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.SelectionMethod
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem.StakingTarget
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.setCustomValidators
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
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
    private val multiStakingSelectionFormatter: MultiStakingSelectionFormatter,
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
            val stakingTypeDetails = stakingTypesDataFlow.first()[position]
                .stakingTypeDetails
            val stakingTypeMixin = editingStakingTypeSelectionMixin.first()
            val enteredAmount = stakingTypeMixin.enteredAmount() ?: return@launch

            validationExecutor.requireValid(
                validationSystem = stakingTypeMixin.getValidationSystem(stakingTypeDetails.stakingType) ?: return@launch,
                payload = EditingStakingTypePayload(enteredAmount, stakingTypeDetails.stakingType, stakingTypeDetails.minStake),
                validationFailureTransformer = { handleSetupStakingTypeValidationFailure(chainAsset, it, resourceManager) },
            ) {
                setRecommendedSelection(stakingTypeDetails.stakingType)
            }
        }
    }

    fun stakingTargetClicked(position: Int) {
        launch {
            val validators = editableSelectionStoreProvider.getSelectionStore(viewModelScope)
                .getValidatorsOrEmpty()
            setupStakingSharedState.set(SetupStakingProcess.ReadyToSubmit(validators, SelectionMethod.CUSTOM))
            router.openSelectCustomValidators()
        }
    }

    private fun setRecommendedSelection(stakingType: Chain.Asset.StakingType) {
        launch {
            editingStakingTypeSelectionMixin.first().setRecommendedSelection(stakingType)
        }
    }

    private suspend fun mapStakingTypes(
        asset: Asset,
        stakingTypesDetails: List<EditableStakingType>,
        selection: RecommendableMultiStakingSelection
    ): List<EditableStakingTypeRVItem> {
        return stakingTypesDetails.mapNotNull {
            val stakingTarget = StakingTarget.Model(multiStakingSelectionFormatter.formatForStakingType(selection))
            val selectedStakingType = selection.selection.stakingOption.stakingType
            val stakingType = it.stakingTypeDetails.stakingType
            when {
                stakingType.isDirectStaking() -> EditableStakingTypeRVItem(
                    isSelected = selectedStakingType.isDirectStaking(),
                    isSelectable = it.isAvailable,
                    title = resourceManager.getString(R.string.setup_staking_type_direct_staking),
                    imageRes = R.drawable.ic_pool_staking_banner_picture,
                    conditions = mapConditions(asset, it.stakingTypeDetails),
                    stakingTarget = stakingTarget.takeIf { selectedStakingType.isDirectStaking() }
                )
                stakingType.isPoolStaking() -> EditableStakingTypeRVItem(
                    isSelected = selectedStakingType.isPoolStaking(),
                    isSelectable = it.isAvailable,
                    title = resourceManager.getString(R.string.setup_staking_type_pool_staking),
                    imageRes = R.drawable.ic_direct_staking_banner_picture,
                    conditions = mapConditions(asset, it.stakingTypeDetails),
                    stakingTarget = stakingTarget.takeIf { selectedStakingType.isPoolStaking() }
                )
                else -> null
            }
        }
    }

    private fun mapConditions(asset: Asset, stakingTypeDetails: StakingTypeDetails): List<String> {
        return buildList {
            val minAmount = mapAmountToAmountModel(stakingTypeDetails.minStake, asset.token)
            add(resourceManager.getString(R.string.setup_staking_type_min_amount_condition, minAmount.token))

            val payoutCondition = when (stakingTypeDetails.payoutType) {
                is PayoutType.Automatically -> resourceManager.getString(R.string.setup_staking_type_payout_type_automatically_condition)
                is PayoutType.Manual -> resourceManager.getString(R.string.setup_staking_type_payout_type_manual_condition)
            }
            add(payoutCondition)

            if (stakingTypeDetails.participationInGovernance) {
                add(resourceManager.getString(R.string.setup_staking_type_governance_condition))
            }

            if (stakingTypeDetails.advancedOptionsAvailable) {
                add(resourceManager.getString(R.string.setup_staking_type_advanced_options_condition))
            }
        }
    }
}

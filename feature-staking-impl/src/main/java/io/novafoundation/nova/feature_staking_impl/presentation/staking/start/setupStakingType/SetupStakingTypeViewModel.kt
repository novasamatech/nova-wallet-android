package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.EditingStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.EditableStakingType
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter.EditableStakingTypeRVItem.StakingTarget
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.isDirectStaking
import io.novafoundation.nova.runtime.ext.isPoolStaking
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SetupStakingTypeViewModel(
    private val router: StakingRouter,
    private val interactor: SetupStakingTypeInteractor,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val resourceManager: ResourceManager,
    payload: SetupAmountMultiStakingPayload,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editingStakingTypeSelectionMixinFactory: EditingStakingTypeSelectionMixinFactory,
    private val multiStakingSelectionFormatter: MultiStakingSelectionFormatter
) : BaseViewModel() {

    private val editingStakingTypeSelectionMixin = editingStakingTypeSelectionMixinFactory.create(viewModelScope)

    private val currentSelectionFlow = editingStakingTypeSelectionMixin.currentSelectionFlow
        .shareInBackground()

    private val assetFlow = assetUseCase.assetFlow(
        payload.availableStakingOptions.chainId,
        payload.availableStakingOptions.assetId
    )

    private val stakingTypesDataFlow = interactor.getEditableStakingTypes()
        .shareInBackground()

    val availableToRewriteData = editingStakingTypeSelectionMixin.availableToRewriteData
        .shareInBackground()

    val stakingTypeModels = combine(
        assetFlow,
        stakingTypesDataFlow,
        editingStakingTypeSelectionMixin.editableSelectionFlow
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
        //TODO: request access to close

        router.back()
    }

    fun donePressed() {
        launch {
            editingStakingTypeSelectionMixin.apply()

            router.back()
        }
    }

    fun selectStakingType(position: Int) {
        launch {
            try {
                val stakingTypeDetails = stakingTypesDataFlow.first()
                editingStakingTypeSelectionMixin.setRecommendedSelection(stakingTypeDetails[position].stakingType)
            } catch (e: Exception) {

            }
        }
    }

    private suspend fun mapStakingTypes(
        asset: Asset,
        stakingTypesDetails: List<EditableStakingType>,
        selection: RecommendableMultiStakingSelection
    ): List<EditableStakingTypeRVItem> {
        return stakingTypesDetails.mapNotNull {
            val stakingTarget = StakingTarget.Model(multiStakingSelectionFormatter.formatForStakingType(selection))
            when {
                it.stakingType.isDirectStaking() -> EditableStakingTypeRVItem(
                    isSelected = selection.selection.stakingOption.stakingType.isDirectStaking(),
                    isSelectable = it.isAvailable,
                    title = resourceManager.getString(R.string.setup_staking_type_pool_staking),
                    imageRes = R.drawable.ic_pool_staking_banner_picture,
                    conditions = mapConditions(asset, it.stakingTypeDetails),
                    stakingTarget = stakingTarget
                )
                it.stakingType.isPoolStaking() -> EditableStakingTypeRVItem(
                    isSelected = selection.selection.stakingOption.stakingType.isPoolStaking(),
                    isSelectable = it.isAvailable,
                    title = resourceManager.getString(R.string.setup_staking_type_direct_staking),
                    imageRes = R.drawable.ic_direct_staking_banner_picture,
                    conditions = mapConditions(asset, it.stakingTypeDetails),
                    stakingTarget = stakingTarget
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
                resourceManager.getString(R.string.setup_staking_type_governance_condition)
            }

            if (stakingTypeDetails.advancedOptionsAvailable) {
                resourceManager.getString(R.string.setup_staking_type_advanced_options_condition)
            }
        }
    }
}

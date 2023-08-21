package io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.staking.setupStakingType.SetupStakingTypeInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.setupStakingType.model.EditableStakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.setupStakingType.model.StakingTypeEditingState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType.adapter.EditableStakingTypeRVItem
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SetupStakingTypeViewModel(
    private val router: StakingRouter,
    private val interactor: SetupStakingTypeInteractor,
    private val walletInteractor: WalletRepository,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val assetFlow = flowOf(Asset())

    private val stakingTypesDataFlow = interactor.getEditableStakingTypes()
        .shareInBackground()

    val availableToRewriteData = stakingTypesDataFlow.map { it.dataHasChanged }
        .shareInBackground()

    val stakingTypeModels = combine(assetFlow, stakingTypesDataFlow) { asset, stakingTypesData ->
        stakingTypesData.map { mapStakingTypes(asset, it) }
    }
        .shareInBackground()

    fun backPressed() {
        //TODO: request access to close

        router.back()
    }

    fun donePressed() {
        interactor.apply()

        router.back()
    }

    fun selectPoolStaking() {
        try {
            interactor.selectPoolStaking()
        } catch (e: Exception) {

        }
    }

    fun selectStakingType(item: EditableStakingTypeRVItem) {
        try {
            when (item.type) {
                EditableStakingTypeRVItem.Type.POOL -> interactor.selectPoolStaking()
                EditableStakingTypeRVItem.Type.DIRECT -> interactor.selectDirectStaking()
            }
        } catch (e: Exception) {

        }
    }

    private fun mapStakingTypes(asset: Asset, stakingTypeEditingState: StakingTypeEditingState): List<EditableStakingTypeRVItem> {
        return stakingTypeEditingState.editableStakingTypes.map {
            when (it) {
                is EditableStakingType.DirectStaking -> EditableStakingTypeRVItem(
                    isSelected = it.isSelected,
                    isSelectable = it.isAvailable,
                    title = resourceManager.getString(R.string.setup_staking_type_pool_staking),
                    imageRes = R.drawable.ic_pool_staking_banner_picture,
                    conditions = mapConditions(asset, it),
                    stakingTarget = null,
                    type = EditableStakingTypeRVItem.Type.DIRECT
                )
                is EditableStakingType.PoolStaking -> EditableStakingTypeRVItem(
                    isSelected = it.isSelected,
                    isSelectable = it.isAvailable,
                    title = resourceManager.getString(R.string.setup_staking_type_direct_staking),
                    imageRes = R.drawable.ic_direct_staking_banner_picture,
                    conditions = mapConditions(asset, it),
                    stakingTarget = null,
                    type = EditableStakingTypeRVItem.Type.POOL
                )
            }
        }
    }

    private fun mapConditions(asset: Asset, editableStakingType: EditableStakingType): List<String> {
        return buildList {
            val minAmount = mapAmountToAmountModel(editableStakingType.minStakeAmount, asset.token)
            add(resourceManager.getString(R.string.setup_staking_type_min_amount_condition, minAmount.token))

            val payoutCondition = when (editableStakingType.payoutType) {
                is PayoutType.Automatically -> resourceManager.getString(R.string.setup_staking_type_payout_type_automatically_condition)
                is PayoutType.Manual -> resourceManager.getString(R.string.setup_staking_type_payout_type_manual_condition)
            }
            add(payoutCondition)

            if (editableStakingType.reusableInGovernance) {
                resourceManager.getString(R.string.setup_staking_type_governance_condition)
            }

            if (editableStakingType.advancedOptionsAvailable) {
                resourceManager.getString(R.string.setup_staking_type_advanced_options_condition)
            }
        }
    }
}

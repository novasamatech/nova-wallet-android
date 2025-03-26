package io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PayoutDetailsViewModel(
    private val interactor: StakingInteractor,
    private val router: StakingRouter,
    private val payout: PendingPayoutParcelable,
    private val addressModelGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val resourceManager: ResourceManager,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
) : BaseViewModel(), ExternalActions.Presentation by externalActions {

    val payoutDetails = interactor.currentAssetFlow()
        .map(::mapPayoutParcelableToPayoutDetailsModel)
        .inBackground()
        .asLiveData()

    fun backClicked() {
        router.back()
    }

    fun payoutClicked() {
        val payload = ConfirmPayoutPayload(
            totalRewardInPlanks = payout.amountInPlanks,
            payouts = listOf(payout)
        )

        router.openConfirmPayout(payload)
    }

    fun validatorExternalActionClicked() = launch {
        externalActions.showAddressActions(payout.validatorInfo.address, selectedAssetState.chain())
    }

    private suspend fun mapPayoutParcelableToPayoutDetailsModel(asset: Asset): PayoutDetailsModel {
        val addressModel = with(payout.validatorInfo) {
            addressModelGenerator.createAccountAddressModel(selectedAssetState.chain(), address, identityName)
        }

        return PayoutDetailsModel(
            validatorAddressModel = addressModel,
            timeLeft = payout.timeLeft,
            timeLeftCalculatedAt = payout.timeLeftCalculatedAt,
            eraDisplay = resourceManager.getString(R.string.staking_era_index_no_prefix, payout.era.toLong()),
            reward = mapAmountToAmountModel(payout.amountInPlanks, asset),
            timerColor = if (payout.closeToExpire) R.color.text_negative else R.color.text_primary,
        )
    }
}

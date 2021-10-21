package io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.rewards.DAYS_IN_YEAR
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculator
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.RewardSuffix
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapPeriodReturnsToRewardEstimation
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.math.BigDecimal

class RewardDestinationProvider(
    private val resourceManager: ResourceManager,
    private val interactor: StakingInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val appLinksProvider: AppLinksProvider,
    private val sharedState: StakingSharedState,
    private val accountDisplayUseCase: AddressDisplayUseCase
) : RewardDestinationMixin.Presentation {

    override val rewardReturnsLiveData = MutableLiveData<RewardDestinationEstimations>()
    override val showDestinationChooserEvent = MutableLiveData<Event<DynamicListBottomSheet.Payload<AddressModel>>>()

    override val rewardDestinationModelFlow = MutableStateFlow<RewardDestinationModel>(RewardDestinationModel.Restake)

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val initialRewardDestination = MutableSharedFlow<RewardDestinationModel>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val rewardDestinationChangedFlow = initialRewardDestination.combine(rewardDestinationModelFlow) { initial, current ->
        initial != current
    }.onStart { emit(false) }

    override fun payoutClicked(scope: CoroutineScope) {
        scope.launch {
            val currentAccount = interactor.getSelectedAccountProjection()

            rewardDestinationModelFlow.value = RewardDestinationModel.Payout(generateDestinationModel(currentAccount))
        }
    }

    override fun payoutTargetClicked(scope: CoroutineScope) {
        val selectedDestination = rewardDestinationModelFlow.value as? RewardDestinationModel.Payout ?: return

        scope.launch {
            val accountsInNetwork = accountsInCurrentNetwork()

            showDestinationChooserEvent.value = Event(DynamicListBottomSheet.Payload(accountsInNetwork, selectedDestination.destination))
        }
    }

    override fun payoutDestinationChanged(newDestination: AddressModel) {
        rewardDestinationModelFlow.value = RewardDestinationModel.Payout(newDestination)
    }

    override fun learnMoreClicked() {
        openBrowserEvent.value = Event(appLinksProvider.payoutsLearnMore)
    }

    override fun restakeClicked() {
        rewardDestinationModelFlow.value = RewardDestinationModel.Restake
    }

    override suspend fun loadActiveRewardDestination(stashState: StakingState.Stash) {
        val rewardDestination = interactor.getRewardDestination(stashState)
        val rewardDestinationModel = mapRewardDestinationToRewardDestinationModel(rewardDestination)

        initialRewardDestination.emit(rewardDestinationModel)
        rewardDestinationModelFlow.value = rewardDestinationModel
    }

    override suspend fun updateReturns(rewardCalculator: RewardCalculator, asset: Asset, amount: BigDecimal) {
        val restakeReturns = rewardCalculator.calculateReturns(amount, DAYS_IN_YEAR, true)
        val payoutReturns = rewardCalculator.calculateReturns(amount, DAYS_IN_YEAR, false)

        val restakeEstimations = mapPeriodReturnsToRewardEstimation(restakeReturns, asset.token, resourceManager, RewardSuffix.APY)
        val payoutEstimations = mapPeriodReturnsToRewardEstimation(payoutReturns, asset.token, resourceManager, RewardSuffix.APR)

        rewardReturnsLiveData.value = RewardDestinationEstimations(restakeEstimations, payoutEstimations)
    }

    private suspend fun mapRewardDestinationToRewardDestinationModel(rewardDestination: RewardDestination): RewardDestinationModel {
        return when (rewardDestination) {
            RewardDestination.Restake -> RewardDestinationModel.Restake
            is RewardDestination.Payout -> {
                val chain = sharedState.chain()
                val addressModel = generateDestinationModel(chain.addressOf(rewardDestination.targetAccountId))

                RewardDestinationModel.Payout(addressModel)
            }
        }
    }

    private suspend fun accountsInCurrentNetwork(): List<AddressModel> {
        return interactor.getAccountProjectionsInSelectedChains()
            .map { generateDestinationModel(it) }
    }

    private suspend fun generateDestinationModel(account: StakingAccount): AddressModel {
        return addressIconGenerator.createAddressModel(account.address, AddressIconGenerator.SIZE_MEDIUM, account.name)
    }

    private suspend fun generateDestinationModel(address: String): AddressModel {
        return addressIconGenerator.createAddressModel(address, AddressIconGenerator.SIZE_MEDIUM, accountDisplayUseCase(address))
    }
}

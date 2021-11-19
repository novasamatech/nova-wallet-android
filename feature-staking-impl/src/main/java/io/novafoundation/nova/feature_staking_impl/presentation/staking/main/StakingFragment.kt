package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.dialog.infoDialog
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.model.NominatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.StashNoneStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.ValidatorStatus
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingNetworkInfoModel
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeSummaryView
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.setupAssetSelector
import kotlinx.android.synthetic.main.fragment_staking.stakingAlertsInfo
import kotlinx.android.synthetic.main.fragment_staking.stakingAssetSelector
import kotlinx.android.synthetic.main.fragment_staking.stakingAvatar
import kotlinx.android.synthetic.main.fragment_staking.stakingContainer
import kotlinx.android.synthetic.main.fragment_staking.stakingEstimate
import kotlinx.android.synthetic.main.fragment_staking.stakingNetworkInfo
import kotlinx.android.synthetic.main.fragment_staking.stakingStakeSummary
import kotlinx.android.synthetic.main.fragment_staking.startStakingBtn
import javax.inject.Inject
import kotlin.time.ExperimentalTime

class StakingFragment : BaseFragment<StakingViewModel>() {

    @Inject protected lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_staking, container, false)
    }

    override fun initViews() {
        stakingContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        stakingEstimate.hideAssetBalanceDollarAmount()

        stakingAvatar.setOnClickListener {
            viewModel.avatarClicked()
        }

        stakingNetworkInfo.storyItemHandler = viewModel::storyClicked
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .stakingComponentFactory()
            .create(this)
            .inject(this)
    }

    @ExperimentalTime
    override fun subscribe(viewModel: StakingViewModel) {
        observeValidations(viewModel)
        setupAssetSelector(stakingAssetSelector, viewModel, imageLoader)

        viewModel.alertsFlow.observe { loadingState ->
            when (loadingState) {
                is LoadingState.Loaded -> {
                    stakingAlertsInfo.hideLoading()

                    if (loadingState.data.isEmpty()) {
                        stakingAlertsInfo.makeGone()
                    } else {
                        stakingAlertsInfo.makeVisible()
                        stakingAlertsInfo.setStatus(loadingState.data)
                    }
                }

                is LoadingState.Loading -> {
                    stakingAlertsInfo.makeVisible()
                    stakingAlertsInfo.showLoading()
                }
            }
        }

        viewModel.stakingViewStateFlow.observe { loadingState ->
            when (loadingState) {
                is LoadingState.Loading -> {
                    startStakingBtn.setVisible(false)
                    stakingEstimate.setVisible(false)
                    stakingStakeSummary.setVisible(false)
                }
                is LoadingState.Loaded -> {
                    val stakingState = loadingState.data

                    startStakingBtn.setVisible(stakingState is WelcomeViewState)
                    stakingEstimate.setVisible(stakingState is WelcomeViewState)
                    stakingStakeSummary.setVisible(stakingState is StakeViewState<*>)

                    when (stakingState) {
                        is NominatorViewState -> {
                            stakingStakeSummary.bindStakeSummary(stakingState, ::mapNominatorStatus)
                        }

                        is ValidatorViewState -> {
                            stakingStakeSummary.bindStakeSummary(stakingState, ::mapValidatorStatus)
                        }

                        is StashNoneViewState -> {
                            stakingStakeSummary.bindStakeSummary(stakingState, ::mapStashNoneStatus)
                        }

                        is WelcomeViewState -> {
                            observeValidations(stakingState)

                            stakingState.assetLiveData.observe {
                                stakingEstimate.loadAssetImage(it.imageUrl)
                                stakingEstimate.setAssetName(it.tokenName)
                                stakingEstimate.setAssetBalance(it.assetBalance)
                            }

                            stakingState.amountFiat.observe { amountFiat ->
                                stakingEstimate.showAssetBalanceDollarAmount()
                                stakingEstimate.setAssetBalanceDollarAmount(amountFiat)
                            }

                            stakingState.returns.observe { rewards ->
                                stakingEstimate.hideReturnsLoading()
                                stakingEstimate.populateMonthEstimation(rewards.monthly)
                                stakingEstimate.populateYearEstimation(rewards.yearly)
                            }

                            stakingEstimate.amountInput.bindTo(stakingState.enteredAmountFlow, viewLifecycleOwner.lifecycleScope)

                            startStakingBtn.setOnClickListener { stakingState.nextClicked() }

                            stakingEstimate.infoActions.setOnClickListener { stakingState.infoActionClicked() }

                            stakingState.showRewardEstimationEvent.observeEvent {
                                StakingRewardEstimationBottomSheet(requireContext(), it).show()
                            }
                        }
                    }
                }
            }
        }

        viewModel.networkInfoStateLiveData.observe { state ->
            when (state) {
                is LoadingState.Loading<*> -> stakingNetworkInfo.showLoading()

                is LoadingState.Loaded<StakingNetworkInfoModel> -> with(state.data) {
                    stakingNetworkInfo.setTotalStake(totalStake, totalStakeFiat)
                    stakingNetworkInfo.setNominatorsCount(nominatorsCount)
                    stakingNetworkInfo.setMinimumStake(minimumStake, minimumStakeFiat)
                    stakingNetworkInfo.setLockupPeriod(lockupPeriod)
                }
            }
        }

        viewModel.stories.observe(stakingNetworkInfo::submitStories)

        viewModel.networkInfoTitle.observe(stakingNetworkInfo::setTitle)

        viewModel.currentAddressModelLiveData.observe {
            stakingAvatar.setImageDrawable(it.image)
        }
    }

    @ExperimentalTime
    private fun <S> StakeSummaryView.bindStakeSummary(
        stakingViewState: StakeViewState<S>,
        mapStatus: (StakeSummaryModel<S>) -> StakeSummaryView.Status,
    ) {
        setStatusClickListener {
            stakingViewState.statusClicked()
        }

        setStakeInfoClickListener {
            stakingViewState.moreActionsClicked()
        }

        stakingViewState.showStatusAlertEvent.observeEvent { (title, message) ->
            showStatusAlert(title, message)
        }

        moreActions.setVisible(stakingViewState.manageStakingActionsButtonVisible)

        stakingViewState.showManageActionsEvent.observeEvent {
            ManageStakingBottomSheet(requireContext(), it, stakingViewState::manageActionChosen).show()
        }

        stakingViewState.stakeSummaryFlow.observe { summaryState ->
            when (summaryState) {
                is LoadingState.Loaded<StakeSummaryModel<S>> -> {
                    val summary = summaryState.data

                    hideLoading()
                    setElectionStatus(mapStatus(summary))
                    setTotalStaked(summary.totalStaked)
                    setTotalRewards(summary.totalRewards)

                    if (summary.totalStakedFiat == null) {
                        hideTotalStakeFiat()
                    } else {
                        showTotalStakedFiat()
                        setTotalStakedFiat(summary.totalStakedFiat)
                    }

                    if (summary.totalRewardsFiat == null) {
                        hideTotalRewardsFiat()
                    } else {
                        showTotalRewardsFiat()
                        setTotalRewardsFiat(summary.totalRewardsFiat)
                    }
                }
            }
        }
    }

    private fun showStatusAlert(title: String, message: String) {
        infoDialog(requireContext()) {
            setTitle(title)
            setMessage(message)
        }
    }

    private fun mapNominatorStatus(summary: NominatorSummaryModel): StakeSummaryView.Status {
        return when (summary.status) {
            is NominatorStatus.Inactive -> StakeSummaryView.Status.Inactive(summary.currentEraDisplay)
            NominatorStatus.Active -> StakeSummaryView.Status.Active(summary.currentEraDisplay)
            is NominatorStatus.Waiting -> StakeSummaryView.Status.Waiting(summary.status.timeLeft)
        }
    }

    private fun mapValidatorStatus(summary: ValidatorSummaryModel): StakeSummaryView.Status {
        return when (summary.status) {
            ValidatorStatus.INACTIVE -> StakeSummaryView.Status.Inactive(summary.currentEraDisplay)
            ValidatorStatus.ACTIVE -> StakeSummaryView.Status.Active(summary.currentEraDisplay)
        }
    }

    private fun mapStashNoneStatus(summary: StashNoneSummaryModel): StakeSummaryView.Status {
        return when (summary.status) {
            StashNoneStatus.INACTIVE -> StakeSummaryView.Status.Inactive(summary.currentEraDisplay)
        }
    }
}

package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
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
import kotlinx.android.synthetic.main.fragment_staking.stakingUserRewards
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
        stakingContainer.applyStatusBarInsets()

        stakingAvatar.setOnClickListener {
            viewModel.avatarClicked()
        }
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
                    stakingEstimate.setVisible(false)
                    stakingUserRewards.setVisible(false)
                    stakingStakeSummary.setVisible(false)
                }

                is LoadingState.Loaded -> {
                    val stakingState = loadingState.data

                    stakingEstimate.setVisible(stakingState is WelcomeViewState)
                    stakingUserRewards.setVisible(stakingState is StakeViewState<*>)
                    stakingStakeSummary.setVisible(stakingState is StakeViewState<*>)

                    stakingNetworkInfo.setExpanded(stakingState is WelcomeViewState)

                    when (stakingState) {
                        is NominatorViewState -> bindStashViews(stakingState, ::mapNominatorStatus)

                        is ValidatorViewState -> bindStashViews(stakingState, ::mapValidatorStatus)

                        is StashNoneViewState -> bindStashViews(stakingState, ::mapStashNoneStatus)

                        is WelcomeViewState -> {
                            observeValidations(stakingState)

                            stakingState.returns.observe { rewardsState ->
                                when (rewardsState) {
                                    is LoadingState.Loaded -> {
                                        val rewards = rewardsState.data

                                        stakingEstimate.showGains(rewards.monthlyPercentage, rewards.yearlyPercentage)
                                    }

                                    is LoadingState.Loading -> stakingEstimate.showLoading()
                                }
                            }

                            stakingEstimate.startStakingButton.setOnClickListener { stakingState.nextClicked() }

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
                    with(stakingNetworkInfo) {
                        setTotalStaked(totalStaked)
                        setNominatorsCount(activeNominators)
                        setMinimumStake(minimumStake)
                        setUnstakingPeriod(unstakingPeriod)
                        setStakingPeriod(stakingPeriod)
                    }
                }
            }
        }

        viewModel.currentAddressModelLiveData.observe {
            stakingAvatar.setImageDrawable(it.image)
        }
    }

    private fun <S> bindStashViews(
        stakingViewState: StakeViewState<S>,
        mapStatus: (StakeSummaryModel<S>) -> StakeSummaryView.Status,
    ) {
        bindUserRewards(stakingViewState)

        stakingStakeSummary.bindStakeSummary(stakingViewState, mapStatus)
    }

    private fun bindUserRewards(
        stakingViewState: StakeViewState<*>
    ) {
        stakingViewState.userRewardsFlow.observe {
            when (it) {
                is LoadingState.Loaded -> stakingUserRewards.showValue(it.data)
                is LoadingState.Loading -> stakingUserRewards.showLoading()
            }
        }
    }

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

                    showStakeAmount(summary.totalStaked)
                    showStakeStatus(mapStatus(summary))
                }
                is LoadingState.Loading -> showLoading()
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
            is NominatorStatus.Inactive -> StakeSummaryView.Status.Inactive
            NominatorStatus.Active -> StakeSummaryView.Status.Active
            is NominatorStatus.Waiting -> StakeSummaryView.Status.Waiting(summary.status.timeLeft)
        }
    }

    private fun mapValidatorStatus(summary: ValidatorSummaryModel): StakeSummaryView.Status {
        return when (summary.status) {
            ValidatorStatus.INACTIVE -> StakeSummaryView.Status.Inactive
            ValidatorStatus.ACTIVE -> StakeSummaryView.Status.Active
        }
    }

    private fun mapStashNoneStatus(summary: StashNoneSummaryModel): StakeSummaryView.Status {
        return when (summary.status) {
            StashNoneStatus.INACTIVE -> StakeSummaryView.Status.Inactive
        }
    }
}

package io.novafoundation.nova.feature_staking_impl.presentation.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeViews
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.displayFeeStatus
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakeAmount
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakeConfirm
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakeOriginAccount
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakeRewardDestination
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakeSelectedValidators
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakeSelectedValidatorsCount
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakeToolbar
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakingEachEraLength
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakingFeeFiat
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakingFeeProgress
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakingFeeToken
import kotlinx.android.synthetic.main.fragment_confirm_stake.confirmStakingUnstakingPeriodLength
import kotlinx.android.synthetic.main.fragment_confirm_stake.stakingConfirmationContainer

class ConfirmStakingFragment : BaseFragment<ConfirmStakingViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_stake, container, false)
    }

    override fun initViews() {
        stakingConfirmationContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        confirmStakeToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        confirmStakeOriginAccount.setWholeClickListener { viewModel.originAccountClicked() }

        confirmStakeConfirm.prepareForProgress(viewLifecycleOwner)
        confirmStakeConfirm.setOnClickListener { viewModel.confirmClicked() }

        confirmStakeSelectedValidators.setOnClickListener { viewModel.nominationsClicked() }

        confirmStakeRewardDestination.setPayoutAccountClickListener { viewModel.payoutAccountClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmStakingComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmStakingViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.showNextProgress.observe(confirmStakeConfirm::setProgress)

        viewModel.rewardDestinationLiveData.observe {

            if (it != null) {
                confirmStakeRewardDestination.makeVisible()
                confirmStakeRewardDestination.showRewardDestination(it)
            } else {
                confirmStakeRewardDestination.makeGone()
            }
        }

        viewModel.assetModelLiveData.observe {
            confirmStakeAmount.setAssetBalance(it.assetBalance)
            confirmStakeAmount.setAssetName(it.tokenName)
            confirmStakeAmount.setAssetImageResource(it.tokenIconRes)
        }

        viewModel.feeLiveData.observe {
            displayFeeStatus(
                it,
                FeeViews(confirmStakingFeeProgress, confirmStakingFeeFiat, confirmStakingFeeToken)
            )
        }

        viewModel.currentAccountModelFlow.observe {
            confirmStakeOriginAccount.setMessage(it.nameOrAddress)
            confirmStakeOriginAccount.setTextIcon(it.image)
        }

        viewModel.nominationsLiveData.observe {
            confirmStakeSelectedValidatorsCount.text = it
        }

        viewModel.displayAmountLiveData.observe { bondedAmount ->
            confirmStakeAmount.setVisible(bondedAmount != null)

            bondedAmount?.let { confirmStakeAmount.amountInput.setText(it.toString()) }
        }

        viewModel.unstakingTime.observe {
            confirmStakingUnstakingPeriodLength.text = it
        }

        viewModel.eraHoursLength.observe {
            confirmStakingEachEraLength.text = it
        }
    }
}

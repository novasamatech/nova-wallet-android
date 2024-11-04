package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmRewardDestinationBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.ConfirmRewardDestinationPayload

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class ConfirmRewardDestinationFragment : BaseFragment<ConfirmRewardDestinationViewModel, FragmentConfirmRewardDestinationBinding>() {

    companion object {

        fun getBundle(payload: ConfirmRewardDestinationPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override fun createBinding() = FragmentConfirmRewardDestinationBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmRewardDestinationContainer.applyStatusBarInsets()

        binder.confirmRewardDestinationToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.confirmRewardDestinationExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.confirmRewardDestinationConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmRewardDestinationConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.confirmRewardDestinationRewardDestination.setPayoutAccountClickListener { viewModel.payoutAccountClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmRewardDestinationPayload>(KEY_PAYLOAD)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmRewardDestinationFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmRewardDestinationViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.showNextProgress.observe(binder.confirmRewardDestinationConfirm::setProgressState)

        viewModel.rewardDestinationFlow.observe(binder.confirmRewardDestinationRewardDestination::showRewardDestination)

        viewModel.walletUiFlow.observe(binder.confirmRewardDestinationExtrinsicInformation::setWallet)
        viewModel.feeStatusFlow.observe(binder.confirmRewardDestinationExtrinsicInformation::setFeeStatus)
        viewModel.originAccountModelFlow.observe(binder.confirmRewardDestinationExtrinsicInformation::setAccount)
    }
}

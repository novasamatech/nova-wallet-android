package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmSetControllerBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmSetControllerFragment : BaseFragment<ConfirmSetControllerViewModel, FragmentConfirmSetControllerBinding>() {

    companion object {
        fun getBundle(payload: ConfirmSetControllerPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentConfirmSetControllerBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmSetControllerToolbar.applyStatusBarInsets()

        binder.confirmSetControllerToolbar.setHomeButtonListener { viewModel.back() }

        binder.confirmSetControllerConfirm.setOnClickListener { viewModel.confirmClicked() }
        binder.confirmSetControllerConfirm.prepareForProgress(viewLifecycleOwner)

        binder.confirmSetControllerExtrinsicInformation.setOnAccountClickedListener { viewModel.stashClicked() }

        binder.confirmSetControllerController.setOnClickListener { viewModel.controllerClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmSetControllerPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmSetControllerFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmSetControllerViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.walletUiFlow.observe(binder.confirmSetControllerExtrinsicInformation::setWallet)
        viewModel.stashAddressFlow.observe(binder.confirmSetControllerExtrinsicInformation::setAccount)
        viewModel.feeStatusFlow.observe(binder.confirmSetControllerExtrinsicInformation::setFeeStatus)

        viewModel.controllerAddressLiveData.observe(binder.confirmSetControllerController::showAddress)

        viewModel.submittingInProgress.observe(binder.confirmSetControllerConfirm::setProgressState)
    }
}

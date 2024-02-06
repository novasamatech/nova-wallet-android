package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerConfirm
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerController
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerExtrinsicInformation
import kotlinx.android.synthetic.main.fragment_confirm_set_controller.confirmSetControllerToolbar

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmSetControllerFragment : BaseFragment<ConfirmSetControllerViewModel>() {
    companion object {
        fun getBundle(payload: ConfirmSetControllerPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_set_controller, container, false)
    }

    override fun initViews() {
        confirmSetControllerToolbar.applyStatusBarInsets()

        confirmSetControllerToolbar.setHomeButtonListener { viewModel.back() }

        confirmSetControllerConfirm.setOnClickListener { viewModel.confirmClicked() }
        confirmSetControllerConfirm.prepareForProgress(viewLifecycleOwner)

        confirmSetControllerExtrinsicInformation.setOnAccountClickedListener { viewModel.stashClicked() }

        confirmSetControllerController.setOnClickListener { viewModel.controllerClicked() }
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

        viewModel.walletUiFlow.observe(confirmSetControllerExtrinsicInformation::setWallet)
        viewModel.stashAddressFlow.observe(confirmSetControllerExtrinsicInformation::setAccount)
        viewModel.feeStatusFlow.observe(confirmSetControllerExtrinsicInformation::setFeeStatus)

        viewModel.controllerAddressLiveData.observe(confirmSetControllerController::showAddress)

        viewModel.submittingInProgress.observe(confirmSetControllerConfirm::setProgress)
    }
}

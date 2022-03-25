package io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.set

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.presenatation.account.chooser.AccountChooserBottomSheetDialog
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_set_controller_account.confirmSetControllerContainer
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerAdvertisement
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerContinue
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerController
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerStash
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerSwitchToStashWarning
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerToolbar

class SetControllerFragment : BaseFragment<SetControllerViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_set_controller_account, container, false)
    }

    override fun initViews() {
        confirmSetControllerContainer.applyStatusBarInsets()

        setControllerContinue.setOnClickListener { viewModel.continueClicked() }
        setControllerContinue.prepareForProgress(viewLifecycleOwner)

        setControllerStash.setOnClickListener { viewModel.stashClicked() }
        setControllerController.setOnClickListener { viewModel.controllerClicked() }

        setControllerAdvertisement.setOnLearnMoreClickedListener { viewModel.onMoreClicked() }
        setControllerToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .setControllerFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SetControllerViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.stashAccountModel.observe(setControllerStash::setAddressModel)
        viewModel.controllerAccountModel.observe(setControllerController::setAddressModel)

        viewModel.chooseControllerAction.awaitableActionLiveData.observeEvent {
            AccountChooserBottomSheetDialog(
                context = requireContext(),
                payload = it.payload,
                onSuccess = it.onSuccess,
                onCancel = it.onCancel,
                title = R.string.staking_controller_account
            ).show()
        }

        viewModel.ableToChangeController.observe { ableToChangeController ->
            val controllerViewAction = R.drawable.ic_chevron_down.takeIf { ableToChangeController }
            setControllerController.setActionIcon(controllerViewAction)
            setControllerController.isEnabled = ableToChangeController

            setControllerSwitchToStashWarning.setVisible(!ableToChangeController)
        }

        viewModel.continueButtonState.observe(setControllerContinue::setState)
    }
}

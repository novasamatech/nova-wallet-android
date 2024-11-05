package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.account.chooser.AccountChooserBottomSheetDialog
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSetControllerAccountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SetControllerFragment : BaseFragment<SetControllerViewModel, FragmentSetControllerAccountBinding>() {

    override fun createBinding() = FragmentSetControllerAccountBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmSetControllerContainer.applyStatusBarInsets()

        binder.setControllerContinue.setOnClickListener { viewModel.continueClicked() }
        binder.setControllerContinue.prepareForProgress(viewLifecycleOwner)

        binder.setControllerStash.setOnClickListener { viewModel.stashClicked() }
        binder.setControllerController.setOnClickListener { viewModel.controllerClicked() }

        binder.setControllerAdvertisement.setOnLearnMoreClickedListener { viewModel.onMoreClicked() }
        binder.setControllerToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.setControllerController.setActionTint(R.color.icon_secondary)
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

        viewModel.stashAccountModel.observe(binder.setControllerStash::setAddressModel)
        viewModel.controllerAccountModel.observe(binder.setControllerController::setAddressModel)

        viewModel.chooseControllerAction.awaitableActionLiveData.observeEvent {
            AccountChooserBottomSheetDialog(
                context = requireContext(),
                payload = it.payload,
                onSuccess = { _, item -> it.onSuccess(item) },
                onCancel = it.onCancel,
                title = R.string.staking_controller_account
            ).show()
        }

        viewModel.isControllerSelectorEnabled.observe { ableToChangeController ->
            val controllerViewAction = R.drawable.ic_chevron_down.takeIf { ableToChangeController }
            binder.setControllerController.setActionIcon(controllerViewAction)
            binder.setControllerController.isEnabled = ableToChangeController
        }

        viewModel.showSwitchToStashWarning.observe(binder.setControllerSwitchToStashWarning::setVisible)

        viewModel.advertisementCardModel.observe(binder.setControllerAdvertisement::setModel)

        viewModel.continueButtonState.observe(binder.setControllerContinue::setState)
    }
}

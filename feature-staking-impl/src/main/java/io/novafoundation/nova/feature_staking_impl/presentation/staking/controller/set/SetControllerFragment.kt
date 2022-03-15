package io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.set

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.feature_account_api.presenatation.account.chooser.AccountChooserBottomSheetDialog
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerContinueBtn
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerDestinationAccount
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerFee
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerLearnMore
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerNotStashWarning
import kotlinx.android.synthetic.main.fragment_set_controller_account.setControllerStashAccount
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
        onBackPressed { viewModel.backClicked() }
        setControllerContinueBtn.setOnClickListener { viewModel.continueClicked() }

        setControllerStashAccount.setWholeClickListener { viewModel.openExternalActions() }
        setControllerDestinationAccount.setWholeClickListener { viewModel.openAccounts() }

        setControllerLearnMore.setOnClickListener { viewModel.onMoreClicked() }
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

        viewModel.stashAccountModel.observe {
            setControllerStashAccount.setTextIcon(it.image)
            setControllerStashAccount.setMessage(it.nameOrAddress)
        }

        viewModel.controllerAccountModel.observe {
            setControllerDestinationAccount.setTextIcon(it.image)
            setControllerDestinationAccount.setMessage(it.nameOrAddress)
        }

        viewModel.feeLiveData.observe(setControllerFee::setFeeStatus)

        setupExternalActions(viewModel)

        viewModel.showControllerChooserEvent.observeEvent(::showControllerChooser)

        viewModel.showNotStashAccountWarning.observe(setControllerNotStashWarning::setVisible)

        viewModel.isContinueButtonAvailable.observe {
            setControllerContinueBtn.isEnabled = it
        }
    }

    private fun showControllerChooser(payload: DynamicListBottomSheet.Payload<AddressModel>) {
        AccountChooserBottomSheetDialog(
            requireContext(),
            payload,
            viewModel::payoutControllerChanged,
            R.string.staking_controller_account
        ).show()
    }
}

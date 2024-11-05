package io.novafoundation.nova.feature_account_impl.presentation.pincode

import android.os.Bundle
import android.widget.Toast

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationOrDenyDialog
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentPincodeBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class PincodeFragment : BaseFragment<PinCodeViewModel, FragmentPincodeBinding>() {

    companion object {
        const val KEY_PINCODE_ACTION = "pincode_action"

        fun getPinCodeBundle(pinCodeAction: PinCodeAction): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PINCODE_ACTION, pinCodeAction)
            }
        }
    }

    override fun createBinding() = FragmentPincodeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideKeyboard()
    }

    override fun inject() {
        val navigationFlow = argument<PinCodeAction>(KEY_PINCODE_ACTION)

        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .pincodeComponentFactory()
            .create(this, navigationFlow)
            .inject(this)
    }

    override fun initViews() {
        binder.toolbar.setHomeButtonListener { viewModel.backPressed() }

        with(binder.pinCodeNumbers) {
            pinCodeEnteredListener = { viewModel.pinCodeEntered(it) }
            fingerprintClickListener = { viewModel.startBiometryAuth() }
        }

        onBackPressed {
            if (viewModel.isBackRoutingBlocked) {
                viewModel.finishApp()
            } else {
                viewModel.backPressed()
            }
        }

        binder.pinCodeNumbers.bindProgressView(binder.pincodeProgress)
    }

    override fun subscribe(viewModel: PinCodeViewModel) {
        setupConfirmationOrDenyDialog(R.style.AccentAlertDialogTheme, viewModel.confirmationAwaitableAction)

        viewModel.pinCodeAction.toolbarConfiguration.titleRes?.let {
            binder.toolbar.setTitle(getString(it))
        }

        viewModel.showFingerPrintEvent.observeEvent {
            binder.pinCodeNumbers.changeBimometricButtonVisibility(it)
        }

        viewModel.biometricEvents.observe {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.homeButtonVisibilityLiveData.observe(binder.toolbar::setHomeButtonVisibility)

        viewModel.matchingPincodeErrorEvent.observeEvent {
            binder.pinCodeNumbers.pinCodeMatchingError()
        }

        viewModel.resetInputEvent.observeEvent {
            binder.pinCodeNumbers.resetInput()
            binder.pinCodeTitle.text = it
        }

        viewModel.startAuth()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}

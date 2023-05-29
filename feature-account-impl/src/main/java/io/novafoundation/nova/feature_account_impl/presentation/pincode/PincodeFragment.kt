package io.novafoundation.nova.feature_account_impl.presentation.pincode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationOrDenyDialog
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_pincode.pinCodeNumbers
import kotlinx.android.synthetic.main.fragment_pincode.pinCodeTitle
import kotlinx.android.synthetic.main.fragment_pincode.pincodeProgress
import kotlinx.android.synthetic.main.fragment_pincode.toolbar

class PincodeFragment : BaseFragment<PinCodeViewModel>() {

    companion object {
        const val KEY_PINCODE_ACTION = "pincode_action"

        fun getPinCodeBundle(pinCodeAction: PinCodeAction): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PINCODE_ACTION, pinCodeAction)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideKeyboard()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pincode, container, false)
    }

    override fun inject() {
        val navigationFlow = argument<PinCodeAction>(KEY_PINCODE_ACTION)

        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .pincodeComponentFactory()
            .create(this, navigationFlow)
            .inject(this)
    }

    override fun initViews() {
        toolbar.setHomeButtonListener { viewModel.backPressed() }

        with(pinCodeNumbers) {
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

        pinCodeNumbers.bindProgressView(pincodeProgress)
    }

    override fun subscribe(viewModel: PinCodeViewModel) {
        setupConfirmationOrDenyDialog(viewModel.confirmationAwaitableAction)

        viewModel.pinCodeAction.toolbarConfiguration.titleRes?.let {
            toolbar.setTitle(getString(it))
        }

        viewModel.showFingerPrintEvent.observeEvent {
            pinCodeNumbers.changeBimometricButtonVisibility(it)
        }

        viewModel.biometricEvents.observe {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.homeButtonVisibilityLiveData.observe(toolbar::setHomeButtonVisibility)

        viewModel.matchingPincodeErrorEvent.observeEvent {
            pinCodeNumbers.pinCodeMatchingError()
        }

        viewModel.resetInputEvent.observeEvent {
            pinCodeNumbers.resetInput()
            pinCodeTitle.text = it
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

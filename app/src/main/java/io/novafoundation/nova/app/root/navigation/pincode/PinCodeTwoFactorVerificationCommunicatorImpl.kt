package io.novafoundation.nova.app.root.navigation.pincode

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BackDelayedNavigation
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.settings.PinCodeTwoFactorVerificationCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.settings.PinCodeTwoFactorVerificationRequester.Request
import io.novafoundation.nova.feature_account_impl.presentation.settings.PinCodeTwoFactorVerificationResponder.Response
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
import kotlinx.coroutines.flow.Flow

class PinCodeTwoFactorVerificationCommunicatorImpl(
    navigationHolder: NavigationHolder
) : BaseInterScreenCommunicator<Request, Response>(navigationHolder), PinCodeTwoFactorVerificationCommunicator {

    override val responseFlow: Flow<Response>
        get() = clearedResponseFlow()

    override fun openRequest(request: Request) {
        val action = PinCodeAction.TwoFactorVerification(BackDelayedNavigation)
        val bundle = PincodeFragment.getPinCodeBundle(action)
        navController.navigate(R.id.action_pin_code_two_factor_verification, bundle)
    }
}

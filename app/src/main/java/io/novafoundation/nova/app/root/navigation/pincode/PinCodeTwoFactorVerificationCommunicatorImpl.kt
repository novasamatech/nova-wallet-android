package io.novafoundation.nova.app.root.navigation.pincode

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationCommunicator
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationRequester.Request
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationResponder.Response
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
import kotlinx.coroutines.flow.Flow

class PinCodeTwoFactorVerificationCommunicatorImpl(
    navigationHolder: NavigationHolder
) : NavStackInterScreenCommunicator<Request, Response>(navigationHolder), PinCodeTwoFactorVerificationCommunicator {

    override val responseFlow: Flow<Response>
        get() = clearedResponseFlow()

    override fun openRequest(request: Request) {
        super.openRequest(request)
        val action = PinCodeAction.TwoFactorVerification(request.useBiometryIfEnabled)
        val bundle = PincodeFragment.getPinCodeBundle(action)
        navController.navigate(R.id.action_pin_code_two_factor_verification, bundle)
    }
}

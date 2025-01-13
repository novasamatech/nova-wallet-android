package io.novafoundation.nova.app.root.navigation.navigators.pincode

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.FlowInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.navigationBuilder
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationCommunicator
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationRequester.Request
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationResponder.Response
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
import kotlinx.coroutines.flow.Flow

class PinCodeTwoFactorVerificationCommunicatorImpl(
    private val navigationHoldersRegistry: NavigationHoldersRegistry
) : FlowInterScreenCommunicator<Request, Response>(), PinCodeTwoFactorVerificationCommunicator {

    override fun dispatchRequest(request: Request) {
        val action = PinCodeAction.TwoFactorVerification(request.useBiometryIfEnabled)
        val bundle = PincodeFragment.getPinCodeBundle(action)

        navigationHoldersRegistry.navigationBuilder().action(R.id.action_pin_code_two_factor_verification)
            .setArgs(bundle)
            .navigateInRoot()
    }
}

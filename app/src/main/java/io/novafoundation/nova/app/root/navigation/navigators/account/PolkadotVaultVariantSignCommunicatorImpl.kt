package io.novafoundation.nova.app.root.navigation.navigators.account

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.getBackStackEntryBefore
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Response
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerPayload

class PolkadotVaultVariantSignCommunicatorImpl(
    navigationHolder: SplitScreenNavigationHolder,
) : NavStackInterScreenCommunicator<Request, Response>(navigationHolder), PolkadotVaultVariantSignCommunicator {

    private var usedPolkadotVaultVariant: PolkadotVaultVariant? = null

    override fun respond(response: Response) {
        val requester = navController.getBackStackEntryBefore(R.id.showSignParitySignerFragment)

        saveResultTo(requester, response)
    }

    override fun setUsedVariant(variant: PolkadotVaultVariant) {
        usedPolkadotVaultVariant = variant
    }

    override fun openRequest(request: Request) {
        super.openRequest(request)

        val payload = ShowSignParitySignerPayload(request, requireNotNull(usedPolkadotVaultVariant))
        val bundle = ShowSignParitySignerFragment.getBundle(payload)
        navController.navigate(R.id.action_open_sign_parity_signer, bundle)
    }
}

package io.novafoundation.nova.app.root.navigation.account

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.getBackStackEntryBefore
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Response
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerPayload

abstract class PolkadotVaultVariantSignCommunicatorImpl(
    private val polkadotVaultVariant: PolkadotVaultVariant,
    navigationHolder: NavigationHolder,
) : NavStackInterScreenCommunicator<Request, Response>(navigationHolder), SignInterScreenCommunicator {

    override fun respond(response: Response) {
        val requester = navController.getBackStackEntryBefore(R.id.showSignParitySignerFragment)

        saveResultTo(requester, response)
    }

    override fun openRequest(request: Request) {
        super.openRequest(request)

        val payload = ShowSignParitySignerPayload(request, polkadotVaultVariant)
        val bundle = ShowSignParitySignerFragment.getBundle(payload)
        navController.navigate(R.id.action_open_sign_parity_signer, bundle)
    }
}

class PolkadotVaultSignCommunicatorImpl(navigationHolder: NavigationHolder) : PolkadotVaultVariantSignCommunicatorImpl(
    polkadotVaultVariant = PolkadotVaultVariant.POLKADOT_VAULT,
    navigationHolder = navigationHolder
), PolkadotVaultSignCommunicator

class ParitySignerSignCommunicatorImpl(navigationHolder: NavigationHolder) : PolkadotVaultVariantSignCommunicatorImpl(
    polkadotVaultVariant = PolkadotVaultVariant.PARITY_SIGNER,
    navigationHolder = navigationHolder
), ParitySignerSignCommunicator

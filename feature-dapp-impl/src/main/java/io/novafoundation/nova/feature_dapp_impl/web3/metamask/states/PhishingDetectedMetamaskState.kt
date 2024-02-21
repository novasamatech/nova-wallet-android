package io.novafoundation.nova.feature_dapp_impl.web3.metamask.states

import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskError
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportRequest
import io.novafoundation.nova.feature_dapp_impl.web3.states.PhishingDetectedState

class PhishingDetectedMetamaskState(override val chain: MetamaskChain) :
    PhishingDetectedState<MetamaskTransportRequest<*>, MetamaskState>(), MetamaskState {

    override val selectedAccountAddress: String? = null

    override fun rejectError(): Throwable {
        return MetamaskError.Rejected()
    }
}

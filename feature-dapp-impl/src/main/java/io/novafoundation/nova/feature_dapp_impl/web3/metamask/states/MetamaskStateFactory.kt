package io.novafoundation.nova.feature_dapp_impl.web3.metamask.states

import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost

class MetamaskStateFactory {

    fun default(hostApi: Web3StateMachineHost): DefaultMetamaskState {
        return DefaultMetamaskState(
            hostApi = hostApi,
            chain = MetamaskChain.ETHEREUM
        )
    }
}

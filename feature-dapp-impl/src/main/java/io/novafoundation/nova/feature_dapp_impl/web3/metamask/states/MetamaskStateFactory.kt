package io.novafoundation.nova.feature_dapp_impl.web3.metamask.states

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.MetamaskInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost

class MetamaskStateFactory(
    private val interactor: MetamaskInteractor,
    private val commonInteractor: DappInteractor,
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val web3Session: Web3Session,
){

    fun default(
        hostApi: Web3StateMachineHost,
        chain: MetamaskChain = MetamaskChain.ETHEREUM,
        selectedAddress: String? = null
    ): DefaultMetamaskState {
        return DefaultMetamaskState(
            interactor = interactor,
            commonInteractor = commonInteractor,
            resourceManager = resourceManager,
            addressIconGenerator = addressIconGenerator,
            web3Session = web3Session,
            hostApi = hostApi,
            chain = chain,
            selectedAccountAddress = selectedAddress,
            stateFactory = this
        )
    }
}

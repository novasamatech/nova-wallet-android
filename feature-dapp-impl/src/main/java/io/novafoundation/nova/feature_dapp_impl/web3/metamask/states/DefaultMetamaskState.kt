package io.novafoundation.nova.feature_dapp_impl.web3.metamask.states

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.MetamaskInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.accept
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskError
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportRequest
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.states.BaseState
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.ExternalEvent
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.StateMachineTransition
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost

class DefaultMetamaskState(
    commonInteractor: DappInteractor,
    resourceManager: ResourceManager,
    addressIconGenerator: AddressIconGenerator,
    web3Session: Web3Session,
    hostApi: Web3StateMachineHost,

    private val stateFactory: MetamaskStateFactory,
    private val interactor: MetamaskInteractor,
    override val chain: MetamaskChain,
    override val selectedAccountAddress: String?
) : BaseState<MetamaskTransportRequest<*>, MetamaskState>(
    commonInteractor = commonInteractor,
    resourceManager = resourceManager,
    addressIconGenerator = addressIconGenerator,
    web3Session = web3Session,
    hostApi = hostApi
), MetamaskState {

    override suspend fun acceptRequest(request: MetamaskTransportRequest<*>, transition: StateMachineTransition<MetamaskState>) {
        when (request) {
            is MetamaskTransportRequest.RequestAccounts -> handleRequestAccounts(request, transition)
            is MetamaskTransportRequest.AddEthereumChain -> handleAddEthereumChain(request, transition)
        }
    }

    override suspend fun acceptEvent(event: ExternalEvent, transition: StateMachineTransition<MetamaskState>) {
        when (event) {
            ExternalEvent.PhishingDetected -> transition.emitState(PhishingDetectedMetamaskState(chain))
        }
    }

    private suspend fun handleAddEthereumChain(
        request: MetamaskTransportRequest.AddEthereumChain,
        transition: StateMachineTransition<MetamaskState>,
    ) = respondIfAllowed(
        ifAllowed = {
            if (chain.chainId == request.chain.chainId) {
                request.accept()
            } else {
                val nextState = stateFactory.default(hostApi, request.chain, selectedAccountAddress)
                transition.emitState(nextState)

                request.accept()

                hostApi.reloadPage()
            }
        },
        ifDenied = {
            request.reject(MetamaskError.Rejected())
        }
    )

    private suspend fun handleRequestAccounts(
        request: MetamaskTransportRequest.RequestAccounts,
        transition: StateMachineTransition<MetamaskState>,
    ) {
        val authorized = authorizeDapp()

        if (authorized) {
            val addresses = interactor.getAddresses(chain.chainId).toList()

            if (addresses.isEmpty()) {
                request.reject(MetamaskError.NoAccounts())
                return
            }

            val selectedAddress = addresses.first()

            val newState = stateFactory.default(hostApi, chain, selectedAddress)
            transition.emitState(newState)

            request.accept(addresses)

            if (selectedAddress != selectedAccountAddress) {
                hostApi.reloadPage()
            }
        } else {
            request.reject(MetamaskError.Rejected())
        }
    }
}


package io.novafoundation.nova.feature_dapp_impl.web3.metamask.states

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.MetamaskInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.accept
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskPersonalSignMessage
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskTransaction
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskTypedMessage
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.chainIdInt
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskError
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportRequest
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session.Authorization
import io.novafoundation.nova.feature_dapp_impl.web3.states.BaseState
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.ExternalEvent
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.StateMachineTransition
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxResponse
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChain
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChainSource
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChainSource.UnknownChainOptions
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmPersonalSignMessage
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTransaction
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTypedMessage
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.toAccountId

class DefaultMetamaskState(
    commonInteractor: DappInteractor,
    resourceManager: ResourceManager,
    addressIconGenerator: AddressIconGenerator,
    web3Session: Web3Session,
    hostApi: Web3StateMachineHost,
    walletUiUseCase: WalletUiUseCase,
    private val stateFactory: MetamaskStateFactory,
    private val interactor: MetamaskInteractor,
    override val chain: MetamaskChain,
    override val selectedAccountAddress: String?
) : BaseState<MetamaskTransportRequest<*>, MetamaskState>(
    commonInteractor = commonInteractor,
    resourceManager = resourceManager,
    addressIconGenerator = addressIconGenerator,
    web3Session = web3Session,
    hostApi = hostApi,
    walletUiUseCase = walletUiUseCase
),
    MetamaskState {

    private val knownChains = mapOf(
        MetamaskChain.ETHEREUM.chainId to MetamaskChain.ETHEREUM,
        MetamaskChain.MOONBEAM.chainId to MetamaskChain.MOONBEAM
    )

    override suspend fun acceptRequest(request: MetamaskTransportRequest<*>, transition: StateMachineTransition<MetamaskState>) {
        when (request) {
            is MetamaskTransportRequest.RequestAccounts -> handleRequestAccounts(request, transition)
            is MetamaskTransportRequest.AddEthereumChain -> handleAddEthereumChain(request, transition)
            is MetamaskTransportRequest.SwitchEthereumChain -> handleSwitchEthereumChain(request, transition)
            is MetamaskTransportRequest.SendTransaction -> handleOperation(request, ::sendTransactionWithConfirmation)
            is MetamaskTransportRequest.SignTypedMessage -> handleOperation(request, ::signTypedMessageWithConfirmation)
            is MetamaskTransportRequest.PersonalSign -> handleOperation(request, ::signPersonalSignWithConfirmation)
        }
    }

    override suspend fun acceptEvent(event: ExternalEvent, transition: StateMachineTransition<MetamaskState>) {
        when (event) {
            ExternalEvent.PhishingDetected -> transition.emitState(PhishingDetectedMetamaskState(chain))
        }
    }

    private suspend fun handleSwitchEthereumChain(
        request: MetamaskTransportRequest.SwitchEthereumChain,
        transition: StateMachineTransition<MetamaskState>
    ) = respondIfAllowed(
        ifAllowed = {
            // already on this chain
            if (request.chainId == chain.chainId) {
                request.accept()
                return@respondIfAllowed
            }

            val knownChain = knownChains[request.chainId]

            if (knownChain != null) {
                val nextState = stateFactory.default(hostApi, knownChain, selectedAccountAddress)
                transition.emitState(nextState)

                request.updateChain(knownChain.chainId, knownChain.rpcUrls.first())
                request.accept()
            } else {
                request.reject(MetamaskError.SwitchChainNotFound(request.chainId))
            }
        },
        ifDenied = {
            request.reject(MetamaskError.Rejected())
        }
    )

    private suspend fun signPersonalSignWithConfirmation(
        request: MetamaskTransportRequest.PersonalSign,
        selectedAddress: String
    ) {
        val hostApiConfirmRequest = ExternalSignRequest.Evm(
            id = request.id,
            payload = EvmSignPayload.PersonalSign(
                message = mapMetamaskPersonalSignMessageToEvm(request.message),
                originAddress = selectedAddress
            )
        )

        confirmOperation(request, hostApiConfirmRequest)
    }

    private suspend fun signTypedMessageWithConfirmation(
        request: MetamaskTransportRequest.SignTypedMessage,
        selectedAddress: String
    ) {
        val hostApiConfirmRequest = ExternalSignRequest.Evm(
            id = request.id,
            payload = EvmSignPayload.SignTypedMessage(
                message = mapMetamaskTypedMessageToEvm(request.message),
                originAddress = selectedAddress
            )
        )

        confirmOperation(request, hostApiConfirmRequest)
    }

    private suspend fun sendTransactionWithConfirmation(
        request: MetamaskTransportRequest.SendTransaction,
        selectedAddress: String,
    ) {
        val selectedAccountId = selectedAddress.asEthereumAddress().toAccountId().value
        val txOriginAccountId = request.transaction.from.asEthereumAddress().toAccountId().value

        if (!selectedAccountId.contentEquals(txOriginAccountId)) {
            request.reject(MetamaskError.AccountsMismatch())
            return
        }

        val hostApiConfirmRequest = ExternalSignRequest.Evm(
            id = request.id,
            payload = EvmSignPayload.ConfirmTx(
                transaction = mapMetamaskTransactionToEvm(request.transaction),
                chainSource = EvmChainSource(
                    evmChainId = chain.chainIdInt(),
                    unknownChainOptions = UnknownChainOptions.WithFallBack(mapMetamaskChainToEvmChain(chain))
                ),
                originAddress = selectedAddress,
                action = EvmSignPayload.ConfirmTx.Action.SEND,
            )
        )

        confirmOperation(request, hostApiConfirmRequest)
    }

    private suspend fun <T : MetamaskTransportRequest<*>> handleOperation(
        request: T,
        onAllowed: suspend (request: T, originAddress: String) -> Unit
    ) {
        val authorizationState = getAuthorizationStateForCurrentPage()

        if (authorizationState == Authorization.State.ALLOWED && selectedAccountAddress != null) {
            // request user confirmation if dapp is authorized
            onAllowed(request, selectedAccountAddress)
        } else {
            // reject otherwise
            request.reject(MetamaskError.Rejected())
        }
    }

    private suspend fun confirmOperation(
        metamaskRequest: MetamaskTransportRequest<String>,
        hostApiConfirmRequest: ExternalSignRequest.Evm
    ) {
        when (val response = hostApi.confirmTx(hostApiConfirmRequest)) {
            is ConfirmTxResponse.Rejected -> metamaskRequest.reject(MetamaskError.Rejected())
            is ConfirmTxResponse.Signed -> metamaskRequest.accept(response.signature)
            is ConfirmTxResponse.Sent -> metamaskRequest.accept(response.txHash)
            is ConfirmTxResponse.SigningFailed -> {
                if (response.shouldPresent) hostApi.showError(resourceManager.getString(R.string.dapp_sign_extrinsic_failed))

                metamaskRequest.reject(MetamaskError.TxSendingFailed())
            }

            is ConfirmTxResponse.ChainIsDisabled -> {
                hostApi.showError(
                    resourceManager.getString(R.string.disabled_chain_error_title, response.chainName),
                    resourceManager.getString(R.string.disabled_chain_error_message, response.chainName)
                )

                metamaskRequest.reject(MetamaskError.TxSendingFailed())
            }
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

                request.updateChain(request.chain.chainId, request.chain.rpcUrls.first())
                request.accept()
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
            val addresses = interactor.getAddresses(chain.chainId)

            if (addresses.isEmpty()) {
                request.reject(MetamaskError.NoAccounts())
                return
            }

            val selectedAddress = addresses.first()

            val newState = stateFactory.default(hostApi, chain, selectedAddress)
            transition.emitState(newState)

            if (selectedAddress != selectedAccountAddress) {
                request.updateAddress(selectedAddress)
            }

            request.accept(addresses)

        } else {
            request.reject(MetamaskError.Rejected())
        }
    }

    private fun mapMetamaskChainToEvmChain(metamaskChain: MetamaskChain): EvmChain {
        return with(metamaskChain) {
            EvmChain(
                chainId = chainId,
                chainName = chainName,
                nativeCurrency = with(nativeCurrency) {
                    EvmChain.NativeCurrency(
                        name = name,
                        symbol = symbol.asTokenSymbol(),
                        decimals = decimals.asPrecision()
                    )
                },
                rpcUrl = rpcUrls.first(),
                iconUrl = iconUrls?.firstOrNull()
            )
        }
    }

    private fun mapMetamaskTransactionToEvm(metamaskTransaction: MetamaskTransaction): EvmTransaction {
        return with(metamaskTransaction) {
            EvmTransaction.Struct(
                gas = gas,
                gasPrice = gasPrice,
                from = from,
                to = to,
                data = data,
                value = value,
                nonce = nonce
            )
        }
    }

    private fun mapMetamaskPersonalSignMessageToEvm(personalSignMessage: MetamaskPersonalSignMessage): EvmPersonalSignMessage {
        return with(personalSignMessage) {
            EvmPersonalSignMessage(data = data)
        }
    }

    private fun mapMetamaskTypedMessageToEvm(typedMessage: MetamaskTypedMessage): EvmTypedMessage {
        return with(typedMessage) {
            EvmTypedMessage(data = data, raw = raw)
        }
    }
}

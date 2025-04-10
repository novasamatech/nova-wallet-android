package io.novafoundation.nova.feature_dapp_impl.web3.metamask.states

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.data.repository.DefaultMetamaskChainRepository
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
    private val walletUiUseCase: WalletUiUseCase,
) {

    fun default(
        hostApi: Web3StateMachineHost,
        chain: MetamaskChain? = null,
        selectedAddress: String? = null
    ): DefaultMetamaskState {
        val usedChain = chain ?: interactor.getDefaultMetamaskChain()

        return DefaultMetamaskState(
            interactor = interactor,
            commonInteractor = commonInteractor,
            resourceManager = resourceManager,
            addressIconGenerator = addressIconGenerator,
            web3Session = web3Session,
            walletUiUseCase = walletUiUseCase,
            hostApi = hostApi,
            chain = usedChain,
            selectedAccountAddress = selectedAddress,
            stateFactory = this
        )
    }
}

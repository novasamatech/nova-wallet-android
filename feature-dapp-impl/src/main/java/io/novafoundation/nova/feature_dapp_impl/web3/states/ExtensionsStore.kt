package io.novafoundation.nova.feature_dapp_impl.web3.states

import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.states.MetamaskStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.states.MetamaskStateMachine
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransport
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransport
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states.PolkadotJsStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states.PolkadotJsStateMachine
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.ExternalEvent
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface ExtensionsStore {

    val polkadotJs: PolkadotJsStateMachine

    val metamask: MetamaskStateMachine
}

class ExtensionStoreFactory(
    private val polkadotJsStateFactory: PolkadotJsStateFactory,
    private val polkadotJsTransportFactory: PolkadotJsTransportFactory,

    private val metamaskStateFactory: MetamaskStateFactory,
    private val metamaskTransportFactory: MetamaskTransportFactory,
) {

    fun create(
        hostApi: Web3StateMachineHost,
        coroutineScope: CoroutineScope
    ): ExtensionsStore {
        val initialPolkadotJsState = polkadotJsStateFactory.default(hostApi)
        val polkadotJsStateMachine: PolkadotJsStateMachine = DefaultWeb3ExtensionStateMachine(initialPolkadotJsState)
        val polkadotJTransport = polkadotJsTransportFactory.create(coroutineScope)

        val initialMetamaskState = metamaskStateFactory.default(hostApi)
        val metamaskStateMachine: MetamaskStateMachine = DefaultWeb3ExtensionStateMachine(initialMetamaskState)
        val metamaskTransport = metamaskTransportFactory.create(coroutineScope)

        return DefaultExtensionsStore(
            polkadotJs = polkadotJsStateMachine,
            polkadotJsTransport = polkadotJTransport,

            metamask = metamaskStateMachine,
            metamaskTransport = metamaskTransport,

            externalEvents = hostApi.externalEvents,
            coroutineScope = coroutineScope
        )
    }
}

private class DefaultExtensionsStore(
    override val polkadotJs: PolkadotJsStateMachine,
    private val polkadotJsTransport: PolkadotJsTransport,

    override val metamask: MetamaskStateMachine,
    private val metamaskTransport: MetamaskTransport,

    private val externalEvents: Flow<ExternalEvent>,
    private val coroutineScope: CoroutineScope
) : ExtensionsStore {

    init {
        polkadotJs wireWith polkadotJsTransport
        metamask wireWith metamaskTransport
    }

    private infix fun <R : Web3Transport.Request<*>, S : State<R, S>> Web3ExtensionStateMachine<S>.wireWith(transport: Web3Transport<R>) {
        transport.requestsFlow
            .onEach { request -> transition { it.acceptRequest(request) } }
            .inBackground()
            .launchIn(coroutineScope)

        externalEvents.onEach { event -> transition { it.acceptEvent(event) } }
            .inBackground()
            .launchIn(coroutineScope)
    }
}

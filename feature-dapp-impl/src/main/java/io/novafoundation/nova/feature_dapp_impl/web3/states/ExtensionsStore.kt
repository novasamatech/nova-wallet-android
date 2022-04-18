package io.novafoundation.nova.feature_dapp_impl.web3.states

import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport
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
}

class ExtensionStoreFactory(
    private val polkadotJsFactory: PolkadotJsStateFactory,
    private val polkadotJsTransportFactory: PolkadotJsTransportFactory,
) {

    fun create(
        hostApi: Web3StateMachineHost,
        coroutineScope: CoroutineScope
    ): ExtensionsStore {
        val polkadotJsStateMachine: PolkadotJsStateMachine = DefaultWeb3ExtensionStateMachine(polkadotJsFactory.default(hostApi))
        val polkadotJsExtension = polkadotJsTransportFactory.create(coroutineScope)

        return DefaultExtensionsStore(
            polkadotJs = polkadotJsStateMachine,
            polkadotJsExtension = polkadotJsExtension,

            externalEvents = hostApi.externalEvents,
            coroutineScope = coroutineScope
        )
    }
}

private class DefaultExtensionsStore(
    override val polkadotJs: PolkadotJsStateMachine,
    private val polkadotJsExtension: PolkadotJsTransport,
    private val externalEvents: Flow<ExternalEvent>,
    private val coroutineScope: CoroutineScope
) : ExtensionsStore {

    init {
        polkadotJs wireWith polkadotJsExtension
    }

    private infix fun <R : Web3Transport.Request<*>, S : State<R, S>> Web3ExtensionStateMachine<S>.wireWith(transport: Web3Transport<R>) {
        transport.requestsFlow
            .onEach { request -> transition { it.acceptRequest(request) } }
            .launchIn(coroutineScope)

        externalEvents.onEach { event -> transition { it.acceptEvent(event) } }
            .launchIn(coroutineScope)
    }
}



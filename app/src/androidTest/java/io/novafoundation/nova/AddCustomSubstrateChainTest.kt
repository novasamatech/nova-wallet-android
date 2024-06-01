package io.novafoundation.nova


import android.util.Log
import com.google.gson.GsonBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.getBlockHash
import io.novafoundation.nova.runtime.network.rpc.stateGetMetadata
import io.novafoundation.nova.runtime.network.rpc.systemChain
import io.novafoundation.nova.runtime.network.rpc.systemChainType
import io.novafoundation.nova.runtime.network.rpc.systemProperties
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.DynamicTypeResolver
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.extentsions.GenericsExtension
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.registry.v14Preset
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.skipAliases
import io.novasama.substrate_sdk_android.runtime.definitions.v14.TypesParserV14
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadataReader
import io.novasama.substrate_sdk_android.runtime.metadata.builder.VersionedRuntimeBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.v14.RuntimeMetadataSchemaV14
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.interceptor.WebSocketResponseInterceptor
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import org.junit.Test
import java.math.BigInteger

class AddCustomSubstrateChainTest : BaseIntegrationTest(allowChainRegistryConnections = false) {

    private val socketService = commonApi.socketServiceCreator()

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    @Test
    fun constructCustomChain() = runTest {
        val rpcUrl = "wss://moonbeam.unitedbloc.com"

        socketService.start(rpcUrl)
        socketService.setInterceptor(object : WebSocketResponseInterceptor {
            override fun onRpcResponseReceived(rpcResponse: RpcResponse): WebSocketResponseInterceptor.ResponseDelivery {
                return WebSocketResponseInterceptor.ResponseDelivery.DELIVER_TO_SENDER
            }
        })

        val systemProperties = socketService.systemProperties()
        val chainId = socketService.getBlockHash(BigInteger.ZERO)

        val chainName = socketService.systemChain()

        val runtime = socketService.fetchRuntimeSnapshot()

        val chain = Chain(
            id = chainId,
            name = chainName,
            assets = listOf(
                Chain.Asset(
                    iconUrl = null,
                    id = 0,
                    priceId = null,
                    chainId = chainId,
                    symbol = systemProperties.tokenSymbol.first(),
                    precision = systemProperties.tokenDecimals.first(),
                    buyProviders = emptyMap(),
                    staking = emptyList(),
                    type = Chain.Asset.Type.Native, // TODO can we determine it?
                    source = Chain.Asset.Source.MANUAL,
                    name = chainName,
                    enabled = true
                )
            ),
            nodes = Chain.Nodes(
                nodeSelectionStrategy = Chain.Nodes.NodeSelectionStrategy.ROUND_ROBIN,
                nodes = listOf(
                    Chain.Node(
                        chainId = chainId,
                        unformattedUrl = rpcUrl,
                        name = "Custom node",
                        orderId = 0
                    )
                )
            ),
            explorers = emptyList(), // TODO we might need explorer link
            externalApis = emptyList(),
            addressPrefix = systemProperties.sss58Format,
            icon = null,
            additional = null,
            types = null,
            isEthereumBased = runtime.isEthereumAddress(),
            isTestNet = identifyIsTestnet(socketService.systemChainType()),
            hasSubstrateRuntime = true,
            pushSupport = false,
            hasCrowdloans = false,
            supportProxy = false,
            governance = emptyList(),
            swap = emptyList(),
            connectionState = Chain.ConnectionState.FULL_SYNC,
            parentId = null // TODO maybe we can fill it?
        )

        Log.d("AddCustomSubstrateChainTest", gson.toJson(chain))
    }

    private suspend fun SocketService.fetchRuntimeSnapshot(): RuntimeSnapshot {
        val metadataHex = stateGetMetadata()
        val metadataReader = RuntimeMetadataReader.read(metadataHex)

        val types = TypesParserV14.parse(
            lookup = metadataReader.metadata[RuntimeMetadataSchemaV14.lookup],
            typePreset = v14Preset(),
        )

        val typeRegistry = TypeRegistry(types, DynamicTypeResolver(DynamicTypeResolver.DEFAULT_COMPOUND_EXTENSIONS + GenericsExtension))
        val runtimeMetadata = VersionedRuntimeBuilder.buildMetadata(metadataReader, typeRegistry)

        return RuntimeSnapshot(typeRegistry, runtimeMetadata)
    }

    private suspend fun RuntimeSnapshot.isEthereumAddress(): Boolean {
        val addressType = typeRegistry["Address"]!!.skipAliases()!!

        return addressType is FixedByteArray && addressType.length == 20
    }

    private fun identifyIsTestnet(chainType: String): Boolean {
        return chainType != "Live"
    }
}

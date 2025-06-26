package io.novafoundation.nova.runtime.multiNetwork.chain.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import java.io.Serializable
import java.math.BigInteger

typealias ChainId = String
typealias ChainAssetId = Int
typealias StringTemplate = String

typealias ExplorerTemplateExtractor = (Chain.Explorer) -> StringTemplate?

typealias TradeProviderId = String
typealias TradeProviderArguments = Map<String, Any?>

data class FullChainAssetId(val chainId: ChainId, val assetId: ChainAssetId) {

    companion object
}

data class Chain(
    val id: ChainId,
    val name: String,
    val assets: List<Asset>,
    val nodes: Nodes,
    val explorers: List<Explorer>,
    val externalApis: List<ExternalApi>,
    val icon: String?,
    val addressPrefix: Int,
    val legacyAddressPrefix: Int?,
    val types: Types?,
    val isEthereumBased: Boolean,
    val isTestNet: Boolean,
    val source: Source,
    val hasSubstrateRuntime: Boolean,
    val pushSupport: Boolean,
    val hasCrowdloans: Boolean,
    val supportProxy: Boolean,
    val governance: List<Governance>,
    val swap: List<Swap>,
    val customFee: List<CustomFee>,
    val connectionState: ConnectionState,
    val parentId: String?,
    val additional: Additional?
) : Identifiable, Serializable {

    companion object // extensions

    val assetsById = assets.associateBy(Asset::id)

    data class Additional(
        val defaultTip: BigInteger?,
        val themeColor: String?,
        val stakingWiki: String?,
        val defaultBlockTimeMillis: Long?,
        val relaychainAsNative: Boolean?,
        val stakingMaxElectingVoters: Int?,
        val feeViaRuntimeCall: Boolean?,
        val supportLedgerGenericApp: Boolean?,
        val identityChain: ChainId?,
        val disabledCheckMetadataHash: Boolean?,
        val sessionLength: Int?
    )

    data class Types(
        val url: String?,
        val overridesCommon: Boolean,
    )

    data class Asset(
        val icon: String?,
        val id: ChainAssetId,
        val priceId: String?,
        val chainId: ChainId,
        val symbol: TokenSymbol,
        val precision: Precision,
        val buyProviders: Map<TradeProviderId, TradeProviderArguments>,
        val sellProviders: Map<TradeProviderId, TradeProviderArguments>,
        val staking: List<StakingType>,
        val type: Type,
        val source: Source,
        val name: String,
        val enabled: Boolean,
    ) : Identifiable, Serializable {

        enum class Source {
            DEFAULT, ERC20, MANUAL
        }

        sealed class Type {
            object Native : Type()

            data class Statemine(
                val id: StatemineAssetId,
                val palletName: String?,
                val isSufficient: Boolean,
            ) : Type()

            data class Orml(
                val currencyIdScale: String,
                val currencyIdType: String,
                val existentialDeposit: BigInteger,
                val transfersEnabled: Boolean,
            ) : Type()

            data class EvmErc20(
                val contractAddress: String
            ) : Type()

            object EvmNative : Type()

            data class Equilibrium(
                val id: BigInteger
            ) : Type()

            object Unsupported : Type()
        }

        enum class StakingType {
            UNSUPPORTED,
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO, // relaychain like
            PARACHAIN, TURING, // parachain-staking like
            NOMINATION_POOLS,
            MYTHOS
        }

        override val identifier = "$chainId:$id"
    }

    data class Nodes(
        val autoBalanceStrategy: AutoBalanceStrategy,
        val wssNodeSelectionStrategy: NodeSelectionStrategy,
        val nodes: List<Node>,
    ) {

        enum class AutoBalanceStrategy {
            ROUND_ROBIN, UNIFORM
        }

        sealed class NodeSelectionStrategy {

            object AutoBalance : NodeSelectionStrategy()

            class SelectedNode(val unformattedNodeUrl: String) : NodeSelectionStrategy()
        }
    }

    data class Node(
        val chainId: ChainId,
        val unformattedUrl: String,
        val name: String,
        val orderId: Int,
        val isCustom: Boolean
    ) : Identifiable {

        enum class ConnectionType {
            HTTPS, WSS, UNKNOWN
        }

        val connectionType = when {
            unformattedUrl.startsWith("wss://") || unformattedUrl.startsWith("ws://") -> ConnectionType.WSS
            unformattedUrl.startsWith("https://") -> ConnectionType.HTTPS
            else -> ConnectionType.UNKNOWN
        }

        override val identifier: String = "$chainId:$unformattedUrl"
    }

    data class Explorer(
        val chainId: ChainId,
        val name: String,
        val account: StringTemplate?,
        val extrinsic: StringTemplate?,
        val event: StringTemplate?
    ) : Identifiable {

        override val identifier = "$chainId:$name"
    }

    sealed class ExternalApi {

        abstract val url: String

        sealed class Transfers : ExternalApi() {

            data class Substrate(override val url: String) : Transfers()

            data class Evm(override val url: String) : Transfers()
        }

        data class Crowdloans(override val url: String) : ExternalApi()

        data class Staking(override val url: String) : ExternalApi()

        data class GovernanceReferenda(override val url: String, val source: Source) : ExternalApi() {

            sealed class Source {

                data class Polkassembly(val network: String?) : Source()

                object SubSquare : Source()
            }
        }

        data class GovernanceDelegations(override val url: String) : ExternalApi()

        data class ReferendumSummary(override val url: String) : ExternalApi()

        data class Multisig(override val url: String) : ExternalApi()
    }

    enum class Governance {
        V1, V2
    }

    enum class Swap {
        ASSET_CONVERSION, HYDRA_DX
    }

    enum class CustomFee {
        ASSET_HUB, HYDRA_DX
    }

    enum class ConnectionState {
        /**
         * Runtime sync is performed for the chain and the chain can be considered ready for any operation
         */
        FULL_SYNC,

        /**
         * Websocket connection is established for the chain, but runtime is not synced.
         * Thus, only runtime-independent operations can be performed
         */
        LIGHT_SYNC,

        /**
         * Chain is completely disabled - it does not initialize websockets not allocates any other resources
         */
        DISABLED
    }

    enum class Source {
        DEFAULT, CUSTOM
    }

    override val identifier: String = id
}

enum class TypesUsage {
    BASE, OWN, BOTH, NONE
}

package io.novafoundation.nova.runtime.multiNetwork.chain.model

import io.novafoundation.nova.common.utils.Identifiable
import java.math.BigInteger

typealias ChainId = String
typealias ChainAssetId = Int
typealias StringTemplate = String

typealias ExplorerTemplateExtractor = (Chain.Explorer) -> StringTemplate?

typealias BuyProviderId = String
typealias BuyProviderArguments = Map<String, Any?>

data class FullChainAssetId(val chainId: ChainId, val assetId: ChainAssetId)

fun FullChainAssetId.toPair() = chainId to assetId

data class Chain(
    val id: ChainId,
    val name: String,
    val assets: List<Asset>,
    val nodes: Nodes,
    val explorers: List<Explorer>,
    val externalApis: List<ExternalApi>,
    val icon: String,
    val addressPrefix: Int,
    val types: Types?,
    val isEthereumBased: Boolean,
    val isTestNet: Boolean,
    val hasSubstrateRuntime: Boolean,
    val hasCrowdloans: Boolean,
    val governance: List<Governance>,
    val swap: List<Swap>,
    val parentId: String?,
    val additional: Additional?
) : Identifiable {

    companion object // extensions

    val assetsById = assets.associateBy(Asset::id)

    data class Additional(
        val defaultTip: BigInteger?,
        val themeColor: String?,
        val stakingWiki: String?,
        val defaultBlockTimeMillis: Long?,
    )

    data class Types(
        val url: String,
        val overridesCommon: Boolean,
    )

    data class Asset(
        val iconUrl: String?,
        val id: ChainAssetId,
        val priceId: String?,
        val chainId: ChainId,
        val symbol: String,
        val precision: Int,
        val buyProviders: Map<BuyProviderId, BuyProviderArguments>,
        val staking: List<StakingType>,
        val type: Type,
        val source: Source,
        val name: String,
        val enabled: Boolean,
    ) : Identifiable {

        enum class Source {
            DEFAULT, ERC20, MANUAL
        }

        sealed class Type {
            object Native : Type()

            data class Statemine(
                val id: StatemineAssetId,
                val palletName: String?
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
            NOMINATION_POOLS
        }

        override val identifier = "$chainId:$id"
    }

    data class Nodes(
        val nodeSelectionStrategy: NodeSelectionStrategy,
        val nodes: List<Node>,
    ) {

        enum class NodeSelectionStrategy {
            ROUND_ROBIN, UNIFORM
        }
    }

    data class Node(
        val chainId: ChainId,
        val unformattedUrl: String,
        val name: String,
        val orderId: Int,
    ) : Identifiable {

        enum class ConnectionType {
            HTTPS, WSS, UNKNOWN
        }

        val connectionType = when {
            unformattedUrl.startsWith("wss://") -> ConnectionType.WSS
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
    }

    enum class Governance {
        V1, V2
    }

    enum class Swap {
        ASSET_CONVERSION
    }

    override val identifier: String = id
}

enum class TypesUsage {
    BASE, OWN, BOTH,
}

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

data class Chain(
    val id: ChainId,
    val name: String,
    val assets: List<Asset>,
    val nodes: List<Node>,
    val explorers: List<Explorer>,
    val externalApi: ExternalApi?,
    val icon: String,
    val addressPrefix: Int,
    val types: Types?,
    val isEthereumBased: Boolean,
    val isTestNet: Boolean,
    val hasCrowdloans: Boolean,
    val governance: Governance,
    val parentId: String?,
    val additional: Additional?
) : Identifiable {

    companion object // extensions

    val assetsBySymbol = assets.associateBy(Asset::symbol)
    val assetsById = assets.associateBy(Asset::id)

    data class Additional(
        val defaultTip: BigInteger?,
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
        val staking: StakingType,
        val type: Type,
        val name: String,
    ) : Identifiable {

        sealed class Type {
            object Native : Type()

            data class Statemine(
                val id: BigInteger,
                val palletName: String?
            ) : Type()

            data class Orml(
                val currencyIdScale: String,
                val currencyIdType: String,
                val existentialDeposit: BigInteger,
                val transfersEnabled: Boolean,
            ) : Type()

            object Unsupported : Type()
        }

        enum class StakingType {
            UNSUPPORTED,
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO, // relaychain like
            PARACHAIN, TURING // parachain-staking like
        }

        override val identifier = "$chainId:$id"
    }

    data class Node(
        val chainId: ChainId,
        val url: String,
        val name: String,
        val orderId: Int,
    ) : Identifiable {

        override val identifier: String = "$chainId:$url"
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

    data class ExternalApi(
        val staking: Section?,
        val history: Section?,
        val crowdloans: Section?
    ) {
        data class Section(val type: Type, val url: String) {
            enum class Type {
                SUBQUERY, GITHUB, UNKNOWN
            }
        }
    }

    enum class Governance {
        V1, V2, NONE
    }

    override val identifier: String = id
}

enum class TypesUsage {
    BASE, OWN, BOTH,
}

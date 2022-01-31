package io.novafoundation.nova.runtime.multiNetwork.chain.model

import java.math.BigInteger

typealias ChainId = String
typealias StringTemplate = String

typealias ExplorerTemplateExtractor = (Chain.Explorer) -> StringTemplate?

data class Chain(
    val id: ChainId,
    val name: String,
    val color: Gradient?,
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
    val parentId: String?,
) {

    companion object // extensions

    val assetsBySymbol = assets.associateBy(Asset::symbol)
    val assetsById = assets.associateBy(Asset::id)

    data class Types(
        val url: String,
        val overridesCommon: Boolean,
    )

    data class Asset(
        val iconUrl: String,
        val id: Int,
        val priceId: String?,
        val chainId: ChainId,
        val symbol: String,
        val precision: Int,
        val staking: StakingType,
        val type: Type,
        val name: String,
    ) {

        sealed class Type {
            object Native : Type()

            data class Statemine(val id: BigInteger) : Type()

            data class Orml(
                val currencyIdScale: String,
                val currencyIdType: String,
                val existentialDeposit: BigInteger,
                val transfersEnabled: Boolean,
            ) : Type()

            object Unsupported : Type()
        }

        enum class StakingType {
            UNSUPPORTED, RELAYCHAIN
        }
    }

    data class Node(
        val url: String,
        val name: String,
    )

    data class Explorer(
        val name: String,
        val account: StringTemplate?,
        val extrinsic: StringTemplate?,
        val event: StringTemplate?
    )

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

    data class Gradient(
        val angle: Float,
        val colors: List<String>,
        val positionsPercent: List<Float>
    )
}

enum class TypesUsage {
    BASE, OWN, BOTH,
}

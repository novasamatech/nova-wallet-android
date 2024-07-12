package io.novafoundation.nova.runtime.explorer

import io.novafoundation.nova.common.utils.Urls

class BlockExplorerLinks(
    val account: String?,
    val event: String?,
    val extrinsic: String?
)

interface BlockExplorerLinkFormatter {

    fun format(link: String): BlockExplorerLinks?
}

class CommonBlockExplorerLinkFormatter(
    private val formatters: List<BlockExplorerLinkFormatter>
) : BlockExplorerLinkFormatter {

    override fun format(link: String): BlockExplorerLinks? {
        return formatters.firstNotNullOfOrNull { it.format(link) }
    }
}

class SubscanBlockExplorerLinkFormatter() : BlockExplorerLinkFormatter {

    override fun format(link: String): BlockExplorerLinks? {
        return try {
            require(link.contains("subscan.io"))

            val normalizedUrl = Urls.normalizeUrl(link)
            return BlockExplorerLinks(
                account = "$normalizedUrl/account/{address}",
                event = null,
                extrinsic = "$normalizedUrl/account/{hash}",
            )
        } catch (e: Exception) {
            null
        }
    }
}

class StatescanBlockExplorerLinkFormatter() : BlockExplorerLinkFormatter {

    override fun format(link: String): BlockExplorerLinks? {
        return try {
            require(link.contains("statescan.io"))

            val normalizedUrl = Urls.normalizeUrl(link)
            return BlockExplorerLinks(
                account = "$normalizedUrl/#/accounts/{address}",
                event = "$normalizedUrl/#/events/{event}",
                extrinsic = "$normalizedUrl/#/extrinsics/{hash}",
            )
        } catch (e: Exception) {
            null
        }
    }
}

class EtherscanBlockExplorerLinkFormatter() : BlockExplorerLinkFormatter {

    override fun format(link: String): BlockExplorerLinks? {
        return try {
            require(link.contains("etherscan.io"))

            val normalizedUrl = Urls.normalizeUrl(link)
            return BlockExplorerLinks(
                account = "$normalizedUrl/address/{address}",
                event = null,
                extrinsic = "$normalizedUrl/tx/{hash}",
            )
        } catch (e: Exception) {
            null
        }
    }
}

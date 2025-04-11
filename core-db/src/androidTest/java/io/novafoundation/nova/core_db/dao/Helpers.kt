package io.novafoundation.nova.core_db.dao

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.model.CurrencyLocal
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

fun createTestChain(
    id: String,
    name: String = id,
    nodesCount: Int = 3,
    assetsCount: Int = 2,
): JoinedChainInfo {
    val chain = chainOf(id, name)
    val nodes = with(chain) {
        (1..nodesCount).map {
            nodeOf("link${it}")
        }
    }
    val assets = with(chain) {
        (1..assetsCount).map {
            assetOf(assetId = it, symbol = it.toString())
        }
    }
    val explorers = emptyList<ChainExplorerLocal>()
    val externalApis = emptyList<ChainExternalApiLocal>()

    return JoinedChainInfo(
        chain,
        NodeSelectionPreferencesLocal(chain.id, autoBalanceEnabled = true, selectedNodeUrl = null),
        nodes,
        assets,
        explorers,
        externalApis
    )
}

fun chainOf(
    id: String,
    name: String = id,
) = ChainLocal(
    id = id,
    parentId = null,
    name = name,
    icon = "Test",
    types = null,
    prefix = 0,
    legacyPrefix = null,
    isTestNet = false,
    isEthereumBased = false,
    hasCrowdloans = false,
    additional = "",
    governance = "governance",
    connectionState = ChainLocal.ConnectionStateLocal.FULL_SYNC,
    pushSupport = true,
    supportProxy = false,
    swap = "",
    hasSubstrateRuntime = true,
    nodeSelectionStrategy = ChainLocal.AutoBalanceStrategyLocal.ROUND_ROBIN,
    source = ChainLocal.Source.CUSTOM,
    customFee = ""
)

fun ChainLocal.nodeOf(
    link: String,
) = ChainNodeLocal(
    name = "Test",
    url = link,
    chainId = id,
    orderId = 0,
    source = ChainNodeLocal.Source.CUSTOM,
)

fun ChainLocal.assetOf(
    assetId: Int,
    symbol: String,
) = ChainAssetLocal(
    name = "Test",
    chainId = id,
    symbol = symbol,
    id = assetId,
    precision = 10,
    priceId = null,
    staking = "test",
    icon = "test",
    type = "test",
    buyProviders = "test",
    sellProviders = "test",
    typeExtras = null,
    enabled = true,
    source = AssetSourceLocal.DEFAULT
)

suspend fun ChainDao.addChains(chains: List<JoinedChainInfo>) {
    applyDiff(
        chainDiff = addedDiff(chains.map(JoinedChainInfo::chain)),
        assetsDiff = addedDiff(chains.flatMap(JoinedChainInfo::assets)),
        nodesDiff = addedDiff(chains.flatMap(JoinedChainInfo::nodes)),
        explorersDiff = addedDiff(chains.flatMap(JoinedChainInfo::explorers)),
        externalApisDiff = addedDiff(chains.flatMap(JoinedChainInfo::externalApis)),
        nodeSelectionPreferencesDiff = emptyDiff()
    )
}

suspend fun ChainDao.addChain(joinedChainInfo: JoinedChainInfo) = addChains(listOf(joinedChainInfo))

suspend fun ChainDao.removeChain(joinedChainInfo: JoinedChainInfo) {
    applyDiff(
        chainDiff = removedDiff(joinedChainInfo.chain),
        assetsDiff = removedDiff(joinedChainInfo.assets),
        nodesDiff = removedDiff(joinedChainInfo.nodes),
        explorersDiff = removedDiff(joinedChainInfo.explorers),
        externalApisDiff = removedDiff(joinedChainInfo.externalApis),
        nodeSelectionPreferencesDiff = emptyDiff()
    )
}

suspend fun ChainDao.updateChain(joinedChainInfo: JoinedChainInfo) {
    applyDiff(
        chainDiff = updatedDiff(joinedChainInfo.chain),
        assetsDiff = updatedDiff(joinedChainInfo.assets),
        nodesDiff = updatedDiff(joinedChainInfo.nodes),
        explorersDiff = updatedDiff(joinedChainInfo.explorers),
        externalApisDiff = updatedDiff(joinedChainInfo.externalApis),
        nodeSelectionPreferencesDiff = emptyDiff()
    )
}

fun <T> addedDiff(elements: List<T>) = CollectionDiffer.Diff(
    added = elements,
    updated = emptyList(),
    removed = emptyList(),
    all = elements
)

fun <T> updatedDiff(elements: List<T>) = CollectionDiffer.Diff(
    added = emptyList(),
    updated = elements,
    removed = emptyList(),
    all = elements
)

fun <T> updatedDiff(element: T) = updatedDiff(listOf(element))

fun <T> addedDiff(element: T) = addedDiff(listOf(element))

fun <T> removedDiff(element: T) = removedDiff(listOf(element))

fun <T> removedDiff(elements: List<T>) = CollectionDiffer.Diff(
    added = emptyList(),
    updated = emptyList(),
    removed = elements,
    all = elements
)

fun <T> emptyDiff() = CollectionDiffer.Diff<T>(emptyList(), emptyList(), emptyList(), emptyList())

fun testMetaAccount(name: String = "Test") = MetaAccountLocal(
    substratePublicKey = byteArrayOf(),
    substrateCryptoType = CryptoType.SR25519,
    ethereumPublicKey = null,
    name = name,
    isSelected = false,
    substrateAccountId = byteArrayOf(),
    ethereumAddress = null,
    position = 0,
    type = MetaAccountLocal.Type.WATCH_ONLY,
    globallyUniqueId = "",
    parentMetaId = 1,
    status = MetaAccountLocal.Status.ACTIVE
)

fun testChainAccount(
    metaId: Long,
    chainId: String,
    accountId: ByteArray = byteArrayOf()
) = ChainAccountLocal(
    metaId = metaId,
    chainId = chainId,
    publicKey = byteArrayOf(),
    cryptoType = CryptoType.SR25519,
    accountId = accountId
)

fun createCurrency(symbol: String = "$", selected: Boolean = true): CurrencyLocal {
    return CurrencyLocal(
        code = "USD",
        name = "Dollar",
        symbol = symbol,
        category = CurrencyLocal.Category.FIAT,
        popular = true,
        id = 0,
        coingeckoId = "usd",
        selected = selected
    )
}

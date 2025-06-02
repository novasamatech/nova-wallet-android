package io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftIssuance
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftPrice
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.UniqueNetworkApi
import io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response.UniqueNetworkNft
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class UniqueNetworkNftProvider(
    private val uniqueNetworkApi: UniqueNetworkApi,
    private val nftDao: NftDao,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) : NftProvider {
    override val requireFullChainSync: Boolean = false

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean) {
        val owner = metaAccount.addressIn(chain) ?: return
        val pageSize = 100
        var offset = 0
        val allNfts = mutableListOf<NftLocal>()

        while (true) {
            val page = uniqueNetworkApi.getNftsPage(
                owner = owner,
                offset = offset,
                limit = pageSize
            )

            if (page.items.isEmpty()) break

            val pageNfts = page.items.map { remote ->
                NftLocal(
                    identifier = nftIdentifier(chain, remote),
                    metaId = metaAccount.id,
                    chainId = chain.id,
                    collectionId = remote.collectionId.toString(),
                    instanceId = remote.key,
                    metadata = null,
                    type = NftLocal.Type.UNIQUE_NETWORK,
                    wholeDetailsLoaded = true,
                    name = remote.name,
                    label = "#${remote.tokenId}",
                    media = remote.image,
                    issuanceType = NftLocal.IssuanceType.UNLIMITED,
                    issuanceTotal = null,
                    issuanceMyEdition = remote.tokenId.toString(),
                    issuanceMyAmount = null,
                    price = null,
                    pricedUnits = null
                )
            }

            allNfts += pageNfts
            offset += pageSize

            if (allNfts.size >= page.count) break
        }

        nftDao.insertNftsDiff(NftLocal.Type.UNIQUE_NETWORK, chain.id, metaAccount.id, allNfts, forceOverwrite)
    }

    override suspend fun nftFullSync(nft: Nft) {
        // do nothing
    }

    override fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return flowOf {
            val nftLocal = nftDao.getNft(nftIdentifier)
            require(nftLocal.wholeDetailsLoaded) {
                "Cannot load details of non fully-synced NFT"
            }

            val chain = chainRegistry.getChain(nftLocal.chainId)
            val metaAccount = accountRepository.getMetaAccount(nftLocal.metaId)

            val collection = uniqueNetworkApi.getCollection(
                collectionId = nftLocal.collectionId.toInt(),
            )

            val issuanceTotal = collection.limits?.token_limit?.toBigInteger() ?: collection.lastTokenId?.toBigInteger()

            val issuanceType = when {
                collection.limits?.token_limit != null -> NftLocal.IssuanceType.LIMITED
                else -> NftLocal.IssuanceType.UNLIMITED
            }

            NftDetails(
                identifier = nftLocal.identifier,
                chain = chain,
                owner = metaAccount.requireAccountIdIn(chain),
                creator = null,
                media = nftLocal.media,
                name = nftLocal.name ?: nftLocal.instanceId!!,
                description = null,
                issuance = nftIssuance(issuanceType, issuanceTotal, nftLocal.issuanceMyEdition, nftLocal.issuanceMyAmount),
                price = nftPrice(nftLocal),
                collection = collection.let {
                    NftDetails.Collection(
                        id = nftLocal.collectionId,
                        name = it.name,
                        media = it.coverImage?.url
                    )
                }
            )
        }
    }

    private fun nftIdentifier(chain: Chain, nft: UniqueNetworkNft): String {
        return "unique-${chain.id}-${nft.collectionId}-${nft.tokenId}"
    }
}

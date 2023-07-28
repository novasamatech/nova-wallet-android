package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.core.model.StorageChange
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftLocalToNftType
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftIssuance
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftPrice
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.network.RmrkV1Api
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class RmrkV1NftProvider(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val api: RmrkV1Api,
    private val nftDao: NftDao
) : NftProvider {

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean) {
        throw UnsupportedOperationException("RmrkV1 not supported")
    }

    override suspend fun subscribeNftOwnerAddress(
        subscriptionBuilder: StorageSharedRequestsBuilder,
        nftLocal: NftLocal
    ): Flow<String> {
        throw UnsupportedOperationException("RmrkV1 doesn't supported")
    }

    override suspend fun nftFullSync(nft: Nft) {
        throw UnsupportedOperationException("RmrkV1 not supported")
    }

    override fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return flowOf {
            val nftLocal = nftDao.getNft(nftIdentifier)
            require(nftLocal.wholeDetailsLoaded) {
                "Cannot load details of non fully-synced NFT"
            }
            val chain = chainRegistry.getChain(nftLocal.chainId)
            val metaAccount = accountRepository.getMetaAccount(nftLocal.metaId)

            NftDetails(
                identifier = nftLocal.identifier,
                chain = chain,
                owner = metaAccount.accountIdIn(chain)!!,
                creator = null,
                media = nftLocal.media,
                name = nftLocal.name!!,
                description = nftLocal.label,
                issuance = nftIssuance(nftLocal),
                price = nftPrice(nftLocal),
                collection = null,
                type = mapNftLocalToNftType(nftLocal)
            )
        }
    }
}

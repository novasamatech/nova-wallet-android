package io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20

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
import io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20.network.Pdc20Api
import io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20.network.Pdc20Listing
import io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20.network.Pdc20Request
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class Pdc20Provider(
    private val api: Pdc20Api,
    private val nftDao: NftDao,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) : NftProvider {

    override val requireFullChainSync: Boolean = false

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean) {
        val address = metaAccount.addressIn(chain) ?: return

        val request = Pdc20Request(address, network = Pdc20Api.NETWORK_POLKADOT)
        val nfts = api.getNfts(request)

        val aggregatedListingsByToken = nfts.data.listings.groupBy { it.token.id }
            .mapValues { (_, listings) ->
                listings.reduce(Pdc20Listing::plus)
            }

        val toSave = nfts.data.userTokenBalances.map { nftRemote ->
            val listing = aggregatedListingsByToken[nftRemote.token.id]

            NftLocal(
                identifier = nftRemote.token.id,
                metaId = metaAccount.id,
                chainId = chain.id,
                collectionId = nftRemote.token.id,
                instanceId = nftRemote.token.id,
                metadata = null,
                type = NftLocal.Type.PDC20,
                wholeDetailsLoaded = true,
                name = nftRemote.token.ticker,
                label = null,
                media = nftRemote.token.logo,
                issuanceType = NftLocal.IssuanceType.FUNGIBLE,
                // We dont know if supply or holding amount can be fractional or not so we are behaving safe
                issuanceTotal = nftRemote.token.totalSupply?.toBigIntegerOrNull(),
                issuanceMyAmount = nftRemote.balance.toBigIntegerOrNull(),
                price = listing?.value?.let { chain.utilityAsset.planksFromAmount(it) },
                pricedUnits = listing?.amount
            )
        }

        nftDao.insertNftsDiff(NftLocal.Type.PDC20, chain.id, metaAccount.id, toSave, forceOverwrite)
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

            NftDetails(
                identifier = nftLocal.identifier,
                chain = chain,
                owner = metaAccount.requireAccountIdIn(chain),
                creator = null,
                media = nftLocal.media,
                name = nftLocal.name ?: nftLocal.instanceId!!,
                description = null,
                issuance = nftIssuance(nftLocal),
                price = nftPrice(nftLocal),
                collection = null // pdc20 token is the same as collection
            )
        }
    }
}

private operator fun Pdc20Listing.plus(other: Pdc20Listing): Pdc20Listing {
    require(this.from.address == other.from.address)
    require(this.token.id == other.token.id)

    return Pdc20Listing(
        from = from,
        token = token,
        amount = amount + other.amount,
        value = value + other.value
    )
}

package io.novafoundation.nova.feature_nft_impl.domain.nft.details

import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Price
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class NftDetailsInteractor(
    private val nftRepository: NftRepository,
    private val tokenRepository: TokenRepository
) {

    fun nftDetailsFlow(nftIdentifier: String): Flow<PricedNftDetails> {
        return nftRepository.nftDetails(nftIdentifier).flatMapLatest { nftDetails ->
            tokenRepository.observeToken(nftDetails.chain.utilityAsset).map { token ->
                PricedNftDetails(
                    nftDetails = nftDetails,
                    price = nftDetails.price?.let {
                        Price(
                            amount = it,
                            token = token
                        )
                    }
                )
            }
        }
    }

    fun isNftTypeSupportedForSend(nftType: Nft.Type, chain: Chain): Boolean {
        return nftRepository.isNftTypeSupportedForSend(nftType, chain)
    }
}

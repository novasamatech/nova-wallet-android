package io.novafoundation.nova.feature_nft_impl.domain.nft.chains

import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@ScreenScope
class NftChainsInteractor @Inject constructor(
    private val nftRepository: NftRepository
) {

    suspend fun getAvailableChains(): List<Chain> {
        return nftRepository.getAvailableChains()
    }
}

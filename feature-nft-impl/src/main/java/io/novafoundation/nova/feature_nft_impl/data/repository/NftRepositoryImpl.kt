package io.novafoundation.nova.feature_nft_impl.data.repository

import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository

class NftRepositoryImpl : NftRepository {

    override fun allNftFlow(): List<Nft> {
        TODO("Not yet implemented")
    }

    override suspend fun initialNftSync() {
        TODO("Not yet implemented")
    }

    override suspend fun fullNftSync(nft: Nft) {
        TODO("Not yet implemented")
    }
}

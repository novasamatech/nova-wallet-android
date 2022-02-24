package io.novafoundation.nova.feature_nft_api.data.repository

import io.novafoundation.nova.feature_nft_api.data.model.Nft

interface NftRepository {

    fun allNftFlow(): List<Nft>

    suspend fun initialNftSync()

    suspend fun fullNftSync(nft: Nft)
}

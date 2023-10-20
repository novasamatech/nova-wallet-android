package io.novafoundation.nova.feature_assets.domain.tokens

import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface AssetsDataCleaner {

    suspend fun clearAssetsData(assetIds: List<FullChainAssetId>)
}

class RealAssetsDataCleaner(
    private val externalBalanceRepository: ExternalBalanceRepository,
    private val contributionsRepository: ContributionsRepository,
    private val walletRepository: WalletRepository,
) : AssetsDataCleaner {

    override suspend fun clearAssetsData(assetIds: List<FullChainAssetId>) {
        contributionsRepository.deleteContributions(assetIds)
        externalBalanceRepository.deleteExternalBalances(assetIds)
        walletRepository.clearAssets(assetIds)
    }
}

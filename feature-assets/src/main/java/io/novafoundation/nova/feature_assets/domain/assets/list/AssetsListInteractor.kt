package io.novafoundation.nova.feature_assets.domain.assets.list

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.isFullySynced
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

private const val PREVIEW_COUNT = 3

class AssetsListInteractor(
    private val accountRepository: AccountRepository,
    private val nftRepository: NftRepository,
    private val assetsViewModeRepository: AssetsViewModeRepository
) {

    fun assetsViewModeFlow() = assetsViewModeRepository.assetsViewModeFlow()

    suspend fun setAssetViewMode(assetViewModel: AssetViewMode) {
        assetsViewModeRepository.setAssetsViewMode(assetViewModel)
    }

    suspend fun fullSyncNft(nft: Nft) = nftRepository.fullNftSync(nft)

    fun observeNftPreviews(): Flow<NftPreviews> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest(nftRepository::allNftFlow)
            .map { nfts ->
                NftPreviews(
                    totalNftsCount = nfts.size,
                    nftPreviews = nfts.sortedBy { it.isFullySynced }.take(PREVIEW_COUNT)
                )
            }
    }
}

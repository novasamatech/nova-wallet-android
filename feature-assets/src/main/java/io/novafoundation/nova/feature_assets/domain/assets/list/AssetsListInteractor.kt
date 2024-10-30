package io.novafoundation.nova.feature_assets.domain.assets.list

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.isFullySynced
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

private const val PREVIEW_COUNT = 3
private const val BANNER_TAG = "CROWDLOAN_UNLOCK_BANNER"

class AssetsListInteractor(
    private val accountRepository: AccountRepository,
    private val nftRepository: NftRepository,
    private val bannerVisibilityRepository: BannerVisibilityRepository,
    private val assetsViewModeService: AssetsViewModeRepository
) {

    fun assetsViewModeFlow() = assetsViewModeService.assetsViewModeFlow()

    suspend fun setAssetViewMode(assetViewModel: AssetViewMode) {
        assetsViewModeService.setAssetsViewMode(assetViewModel)
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

    fun shouldShowCrowdloansBanner(): Flow<Boolean> {
        return bannerVisibilityRepository.shouldShowBannerFlow(BANNER_TAG)
    }

    suspend fun hideCrowdloanBanner() {
        bannerVisibilityRepository.hideBanner(BANNER_TAG)
    }
}

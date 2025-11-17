package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.onEachAsync
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.models.Gift
import io.novafoundation.nova.feature_gift_impl.domain.models.isClaimed
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.sortedBy

interface GiftsInteractor {
    fun observeGifts(fullChainAssetId: FullChainAssetId?): Flow<List<Gift>>

    suspend fun syncGiftsState()
}

class RealGiftsInteractor(
    private val giftsRepository: GiftsRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
    private val selectedAccountUseCase: SelectedAccountUseCase
) : GiftsInteractor {

    override fun observeGifts(fullChainAssetId: FullChainAssetId?): Flow<List<Gift>> = flowOfAll {
        val selectedMetaAccount = selectedAccountUseCase.getSelectedMetaAccount()

        giftsRepository.observeGifts()
            .map { gifts ->
                gifts.filter { it.creatorMetaId == selectedMetaAccount.id }
                    .filter { it.filterByAsset(fullChainAssetId) }
                    .sortedByDescending { it.creationDate.time }
                    .sortedBy { it.status.isClaimed() }
            }
    }

    private fun Gift.filterByAsset(fullChainAssetId: FullChainAssetId?): Boolean {
        if (fullChainAssetId == null) return true

        return chainId == fullChainAssetId.chainId && assetId == fullChainAssetId.assetId
    }

    override suspend fun syncGiftsState() {
        giftsRepository.getGifts()
            .filter { it.status == Gift.Status.PENDING }
            .onEachAsync {
                val (chain, chainAsset) = chainRegistry.chainWithAsset(it.chainId, it.assetId)
                val balanceSource = assetSourceRegistry.sourceFor(chainAsset).balance
                val giftBalance = balanceSource.queryAccountBalance(chain, chainAsset, it.giftAccountId)

                if (giftBalance.total.isZero) {
                    giftsRepository.setGiftState(it.id, Gift.Status.CLAIMED)
                }
            }
    }
}

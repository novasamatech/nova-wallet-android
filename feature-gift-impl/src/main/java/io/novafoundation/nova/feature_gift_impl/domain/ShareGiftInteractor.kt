package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.utils.removeSpacing
import io.novafoundation.nova.feature_gift_impl.data.GiftSecretsRepository
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.models.Gift
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.flow.Flow

interface ShareGiftInteractor {

    fun observeGift(giftId: Long): Flow<Gift>

    suspend fun getGiftSeed(giftId: Long): String

    suspend fun setGiftStateAsReclaimed(id: Long)
}

class RealShareGiftInteractor(
    private val giftsRepository: GiftsRepository,
    private val giftSecretsRepository: GiftSecretsRepository,
) : ShareGiftInteractor {

    override fun observeGift(giftId: Long): Flow<Gift> {
        return giftsRepository.observeGift(giftId)
    }

    override suspend fun getGiftSeed(giftId: Long): String {
        val gift = giftsRepository.getGift(giftId)
        val seed = giftSecretsRepository.getGiftAccountSeed(gift.giftAccountId) ?: error("No secrets for gift found")
        return seed.toHexString()
    }

    override suspend fun setGiftStateAsReclaimed(id: Long) {
        giftsRepository.setGiftState(id, Gift.Status.RECLAIMED)
    }
}

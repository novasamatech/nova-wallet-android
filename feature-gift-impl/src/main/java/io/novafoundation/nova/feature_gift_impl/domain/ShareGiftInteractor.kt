package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.utils.removeSpacing
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.models.Gift
import kotlinx.coroutines.flow.Flow

interface ShareGiftInteractor {

    fun observeGift(giftId: Long): Flow<Gift>

    suspend fun getGiftSeed(giftId: Long): String
}

class RealShareGiftInteractor(
    private val giftsRepository: GiftsRepository,
    private val secretStoreV2: SecretStoreV2
) : ShareGiftInteractor {

    override fun observeGift(giftId: Long): Flow<Gift> {
        return giftsRepository.observeGift(giftId)
    }

    override suspend fun getGiftSeed(giftId: Long): String {
        val gift = giftsRepository.getGift(giftId)
        val secrets = secretStoreV2.getGiftAccountSecrets(gift.giftAccountId) ?: error("No secrets for gift found")
        val seed = secrets.seed ?: error("No seed for gift found")
        return seed.decodeToString()
            .removeSpacing()
    }
}

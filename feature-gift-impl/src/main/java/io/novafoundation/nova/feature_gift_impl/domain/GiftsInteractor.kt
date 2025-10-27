package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.models.Gift
import kotlinx.coroutines.flow.Flow

interface GiftsInteractor {
    fun observeGifts(): Flow<List<Gift>>
}

class RealGiftsInteractor(
    private val giftsRepository: GiftsRepository
) : GiftsInteractor {

    override fun observeGifts(): Flow<List<Gift>> {
        return giftsRepository.observeGifts()
    }
}

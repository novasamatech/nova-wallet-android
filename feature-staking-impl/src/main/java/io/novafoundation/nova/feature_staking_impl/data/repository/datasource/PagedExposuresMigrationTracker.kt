package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.common.utils.indexOfOrNull
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.PagedExposuresMigrationTracker.SavedValue
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface PagedExposuresMigrationTracker {

    sealed class SavedValue<out T> {

        object NotPresent : SavedValue<Nothing>()

        class Present<T>(val value: T): SavedValue<T>()
    }

    suspend fun saveFirstPagedExposuresEra(chainId: ChainId, era: EraIndex)

    suspend fun getFirstPagedExposuresEra(chainId: ChainId): SavedValue<EraIndex?>
}

suspend fun PagedExposuresMigrationTracker.getFirstPagedExposuresEraIndex(chainId: ChainId, historicalRange: List<EraIndex>) : SavedValue<Int?> {
    return getFirstPagedExposuresEra(chainId).map { era ->
        if (era == null) return@map null

        historicalRange.indexOfOrNull(era)
    }
}

inline fun <T, R> PagedExposuresMigrationTracker.SavedValue<T>.map(mapper: (T) -> R): SavedValue<R> {
    return when(this) {
        SavedValue.NotPresent -> SavedValue.NotPresent
        is SavedValue.Present -> SavedValue.Present(mapper(value))
    }
}

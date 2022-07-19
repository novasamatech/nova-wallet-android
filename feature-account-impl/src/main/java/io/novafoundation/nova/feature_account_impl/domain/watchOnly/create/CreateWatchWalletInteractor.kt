package io.novafoundation.nova.feature_account_impl.domain.watchOnly.create

import io.novafoundation.nova.feature_account_impl.data.repository.WatchOnlyRepository
import io.novafoundation.nova.feature_account_impl.data.repository.WatchWalletSuggestion


interface CreateWatchWalletInteractor {

    suspend fun suggestions(): List<WatchWalletSuggestion>
}


class RealCreateWatchWalletInteractor(
    private val repository: WatchOnlyRepository
): CreateWatchWalletInteractor {

    override suspend fun suggestions(): List<WatchWalletSuggestion> {
        return repository.watchWalletSuggestions()
    }
}

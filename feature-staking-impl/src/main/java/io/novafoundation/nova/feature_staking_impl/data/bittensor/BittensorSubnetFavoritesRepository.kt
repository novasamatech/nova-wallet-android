package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BittensorSubnetFavorites(val metaId: Long, val subnetIds: Set<Int>)

class BittensorSubnetFavoritesRepository(
    private val accounts: AccountRepository,
    private val preferences: Preferences
) {

    private val mutex = Mutex()

    fun observeFavorites() = accounts.selectedMetaAccountFlow().flatMapLatest { account ->
        preferences.stringSetFlow(key(account.id)).map { stored ->
            BittensorSubnetFavorites(account.id, stored.orEmpty().mapNotNull(String::toIntOrNull).filter { it in 1..65535 }.toSet())
        }
    }

    suspend fun toggle(metaId: Long, subnetId: BittensorSubnetId) = mutex.withLock {
        check(accounts.getSelectedMetaAccount().id == metaId) { "The selected wallet has changed" }
        val key = key(metaId)
        val value = subnetId.value.toString()
        val saved = preferences.getStringSet(key)
        preferences.putStringSet(key, if (value in saved) saved - value else saved + value)
    }

    private fun key(metaId: Long) = "bittensor_subnet_favorites_" + metaId
}

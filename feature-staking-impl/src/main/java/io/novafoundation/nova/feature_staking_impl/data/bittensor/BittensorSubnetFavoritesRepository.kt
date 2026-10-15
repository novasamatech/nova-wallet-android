package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
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

    fun observeFavorites() = accounts.selectedMetaAccountFlow()
        .flatMapLatest { account ->
            preferences.stringSetFlow(key(account.id))
                .map { stored ->
                    val subnetIds = stored.orEmpty()
                        .mapNotNull(::parseSubnetId)
                        .map { it.value }
                        .toSet()
                    BittensorSubnetFavorites(account.id, subnetIds)
                }
        }

    suspend fun toggle(metaId: Long, subnetId: BittensorNetuid.Subnet) = mutex.withLock {
        check(accounts.getSelectedMetaAccount().id == metaId) { "The selected wallet has changed" }
        val key = key(metaId)
        val value = subnetId.value.toString()
        val saved = preferences.getStringSet(key)
        preferences.putStringSet(key, if (value in saved) saved - value else saved + value)
    }

    private fun key(metaId: Long) = "bittensor_subnet_favorites_" + metaId

    private fun parseSubnetId(value: String): BittensorNetuid.Subnet? {
        val netuid = value.toIntOrNull() ?: return null
        return runCatching { BittensorNetuid.Subnet(netuid) }
            .getOrNull()
    }
}

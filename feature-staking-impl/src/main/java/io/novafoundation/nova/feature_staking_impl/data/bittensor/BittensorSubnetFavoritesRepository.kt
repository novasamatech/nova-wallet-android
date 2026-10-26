package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetReference
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BittensorSubnetFavorites(val metaId: Long, val subnets: Set<BittensorSubnetReference>) {

    operator fun contains(subnet: BittensorSubnet): Boolean = subnets.any {
        it.subnetId == subnet.id && it.registrationBlock == subnet.registrationBlock
    }
}

class BittensorSubnetFavoritesRepository(
    private val accounts: AccountRepository,
    private val preferences: Preferences
) {

    private val mutex = Mutex()

    fun observeFavorites() = accounts.selectedMetaAccountFlow()
        .flatMapLatest { account ->
            preferences.stringSetFlow(key(account.id))
                .map { stored ->
                    val subnets = stored.orEmpty()
                        .mapNotNull(::parseReference)
                        .toSet()
                    BittensorSubnetFavorites(account.id, subnets)
                }
        }

    suspend fun toggle(metaId: Long, subnet: BittensorSubnetReference) = mutex.withLock {
        check(accounts.getSelectedMetaAccount().id == metaId) { "The selected wallet has changed" }
        val key = key(metaId)
        val subnetId = subnet.subnetId.value.toString()
        val value = "$subnetId:${subnet.registrationBlock}"
        val saved = preferences.getStringSet(key)
        val otherSubnets = saved
            .filterNot { it.substringBefore(':') == subnetId }
            .toSet()
        preferences.putStringSet(key, if (value in saved) otherSubnets else otherSubnets + value)
    }

    private fun key(metaId: Long) = "bittensor_subnet_favorites_" + metaId

    private fun parseReference(value: String): BittensorSubnetReference? {
        // Legacy bare ids have no registration evidence. Keep them in storage, but never
        // silently associate them with a possibly re-registered subnet. A new tap replaces them.
        val parts = value.split(':')
        if (parts.size != 2) return null
        val netuid = parts[0].toIntOrNull() ?: return null
        val registrationBlock = parts[1].toBigIntegerOrNull() ?: return null
        return runCatching { BittensorSubnetReference(BittensorNetuid.Subnet(netuid), registrationBlock) }
            .getOrNull()
    }
}

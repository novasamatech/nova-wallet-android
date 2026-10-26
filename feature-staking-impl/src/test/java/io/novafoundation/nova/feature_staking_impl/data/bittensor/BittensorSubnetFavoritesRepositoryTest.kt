package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class BittensorSubnetFavoritesRepositoryTest {

    @Test
    fun `toggle persists subnet only for selected wallet`() = runBlocking {
        val (accounts, preferences) = dependencies(1)
        `when`(preferences.getStringSet("bittensor_subnet_favorites_1")).thenReturn(emptySet())
        val repository = BittensorSubnetFavoritesRepository(accounts, preferences)
        repository.toggle(1, reference(3))
        verify(preferences).putStringSet("bittensor_subnet_favorites_1", setOf("3:100"))
    }

    @Test
    fun `toggling again removes favorite without clearing other subnets`() = runBlocking {
        val (accounts, preferences) = dependencies(1)
        `when`(preferences.getStringSet("bittensor_subnet_favorites_1")).thenReturn(setOf("3:100", "4:200"))
        val repository = BittensorSubnetFavoritesRepository(accounts, preferences)
        repository.toggle(1, reference(3))
        verify(preferences).putStringSet("bittensor_subnet_favorites_1", setOf("4:200"))
    }

    @Test
    fun `favorites flow belongs to wallet and ignores invalid stored ids`() = runBlocking {
        val (accounts, preferences) = dependencies(2)
        val stored = setOf("3:100", "0:100", "-1:100", "bad", "65535:100", "65536:100", "4:-1", "5:18446744073709551616", "6:1:2")
        `when`(preferences.stringSetFlow("bittensor_subnet_favorites_2")).thenReturn(flowOf(stored))
        val repository = BittensorSubnetFavoritesRepository(accounts, preferences)
        val state = repository.observeFavorites().first()
        assertEquals(2L, state.metaId)
        assertEquals(setOf(reference(3), reference(65535)), state.subnets)
    }

    @Test(expected = IllegalStateException::class)
    fun `stale screen cannot edit a different wallet favorites`() {
        runBlocking {
            val (accounts, preferences) = dependencies(2)
            val repository = BittensorSubnetFavoritesRepository(accounts, preferences)
            repository.toggle(1, reference(3))
        }
    }

    @Test
    fun `reused netuid does not inherit the favorite`() {
        val favorites = BittensorSubnetFavorites(1, setOf(reference(3)))
        val subnet = BittensorSubnet(BittensorNetuid.Subnet(3), "Subnet", "alpha", null, registrationBlock = 100.toBigInteger())

        assertTrue(subnet in favorites)
        assertFalse(subnet.copy(registrationBlock = 200.toBigInteger()) in favorites)
        assertFalse(subnet.copy(registrationBlock = null) in favorites)
    }

    @Test
    fun `legacy ids remain unresolved until an explicit selection`() = runBlocking {
        val (accounts, preferences) = dependencies(1)
        `when`(preferences.stringSetFlow("bittensor_subnet_favorites_1")).thenReturn(flowOf(setOf("3", "4")))
        `when`(preferences.getStringSet("bittensor_subnet_favorites_1")).thenReturn(setOf("3", "4"))
        val repository = BittensorSubnetFavoritesRepository(accounts, preferences)

        assertTrue(repository.observeFavorites().first().subnets.isEmpty())
        repository.toggle(1, reference(3))
        verify(preferences).putStringSet("bittensor_subnet_favorites_1", setOf("3:100", "4"))
    }

    @Test
    fun `selecting a new incarnation replaces only that subnet reference`() = runBlocking {
        val (accounts, preferences) = dependencies(1)
        `when`(preferences.getStringSet("bittensor_subnet_favorites_1")).thenReturn(setOf("3:99", "4:200"))
        val repository = BittensorSubnetFavoritesRepository(accounts, preferences)

        repository.toggle(1, reference(3))
        verify(preferences).putStringSet("bittensor_subnet_favorites_1", setOf("3:100", "4:200"))
    }

    private fun reference(netuid: Int) = BittensorSubnetReference(BittensorNetuid.Subnet(netuid), 100.toBigInteger())

    private suspend fun dependencies(metaId: Long): Pair<AccountRepository, Preferences> {
        val account = mock(MetaAccount::class.java)
        `when`(account.id).thenReturn(metaId)
        val accounts = mock(AccountRepository::class.java)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(account)
        `when`(accounts.selectedMetaAccountFlow()).thenReturn(flowOf(account))
        return accounts to mock(Preferences::class.java)
    }
}

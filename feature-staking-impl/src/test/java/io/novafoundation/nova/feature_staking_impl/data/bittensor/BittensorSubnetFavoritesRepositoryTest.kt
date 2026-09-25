package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class BittensorSubnetFavoritesRepositoryTest {

    @Test
    fun `toggle persists subnet only for selected wallet`() = runBlocking {
        val (accounts, preferences) = dependencies(1)
        `when`(preferences.getStringSet("bittensor_subnet_favorites_1")).thenReturn(emptySet())
        BittensorSubnetFavoritesRepository(accounts, preferences).toggle(1, BittensorSubnetId(3))
        verify(preferences).putStringSet("bittensor_subnet_favorites_1", setOf("3"))
    }

    @Test
    fun `toggling again removes favorite without clearing other subnets`() = runBlocking {
        val (accounts, preferences) = dependencies(1)
        `when`(preferences.getStringSet("bittensor_subnet_favorites_1")).thenReturn(setOf("3", "4"))
        BittensorSubnetFavoritesRepository(accounts, preferences).toggle(1, BittensorSubnetId(3))
        verify(preferences).putStringSet("bittensor_subnet_favorites_1", setOf("4"))
    }

    @Test
    fun `favorites flow belongs to wallet and ignores invalid stored ids`() = runBlocking {
        val (accounts, preferences) = dependencies(2)
        `when`(preferences.stringSetFlow("bittensor_subnet_favorites_2")).thenReturn(flowOf(setOf("3", "0", "-1", "bad", "65536")))
        val state = BittensorSubnetFavoritesRepository(accounts, preferences).observeFavorites().first()
        assertEquals(2L, state.metaId)
        assertEquals(setOf(3), state.subnetIds)
    }

    @Test(expected = IllegalStateException::class)
    fun `stale screen cannot edit a different wallet favorites`() {
        runBlocking {
            val (accounts, preferences) = dependencies(2)
            BittensorSubnetFavoritesRepository(accounts, preferences).toggle(1, BittensorSubnetId(3))
        }
    }

    private suspend fun dependencies(metaId: Long): Pair<AccountRepository, Preferences> {
        val account = mock(MetaAccount::class.java)
        `when`(account.id).thenReturn(metaId)
        val accounts = mock(AccountRepository::class.java)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(account)
        `when`(accounts.selectedMetaAccountFlow()).thenReturn(flowOf(account))
        return accounts to mock(Preferences::class.java)
    }
}

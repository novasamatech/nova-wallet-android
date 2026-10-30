package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class BittensorPositionsUpdaterTest {

    private val accounts = mock(AccountRepository::class.java)

    private val scope = AccountUpdateScope(accounts)

    private val registry = mock(ChainRegistry::class.java)

    private val repository = mock(BittensorPositionRepository::class.java)

    private val builder = mock(SharedRequestsBuilder::class.java)

    private val chain = mock(Chain::class.java)

    private val account = mock(MetaAccount::class.java)

    private val updater = BittensorPositionsUpdater(scope, registry, repository)

    @Test
    fun `staking updater observes the shared positions instead of loading another snapshot`() = runBlocking {
        `when`(registry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        val coldKey = ByteArray(32)
        `when`(account.accountIdIn(chain)).thenReturn(coldKey)
        val positions = BittensorStakePositions(emptyList(), emptyList())
        val state = BittensorPositionState(positions, BittensorDataStatus.Fresh)
        `when`(repository.observePositions(chain.id, coldKey)).thenReturn(flowOf(state))

        assertTrue(updater.listenForUpdates(builder, account).toList().isEmpty())

        verify(repository).observePositions(chain.id, coldKey)
        verifyNoInteractions(builder)
    }

    @Test
    fun `wallet without a chain account does not start updates`() = runBlocking {
        `when`(registry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)

        assertTrue(updater.listenForUpdates(builder, account).toList().isEmpty())

        verifyNoInteractions(repository, builder)
    }

    @Test
    fun `bittensor updater is registered only for its staking type`() {
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        val emptyGroup = StakingUpdaters.Group()
        val bittensorGroup = StakingUpdaters.Group(updater)
        val stakingUpdaters = StakingUpdaters(
            relaychainUpdaters = emptyGroup,
            parachainUpdaters = emptyGroup,
            commonUpdaters = emptyGroup,
            turingExtraUpdaters = emptyGroup,
            nominationPoolsUpdaters = emptyGroup,
            mythosUpdaters = emptyGroup,
            bittensorUpdaters = bittensorGroup
        )

        val registered = stakingUpdaters.getUpdaters(chain, Chain.Asset.StakingType.BITTENSOR_ROOT)
        assertEquals(setOf(updater), registered[chain.id])
        assertFalse(stakingUpdaters.getUpdaters(chain, Chain.Asset.StakingType.UNSUPPORTED).containsKey(chain.id))
    }
}

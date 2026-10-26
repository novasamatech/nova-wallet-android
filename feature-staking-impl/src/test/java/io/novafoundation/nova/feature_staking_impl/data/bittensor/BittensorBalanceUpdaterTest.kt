package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class BittensorBalanceUpdaterTest {

    private val scope = mock(AccountUpdateScope::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val account = mock(MetaAccount::class.java)

    private val positions = mock(BittensorPositionRepository::class.java)

    private val subscription = mock(SharedRequestsBuilder::class.java)

    private val daoDelegate = mock(ExternalBalanceDao::class.java)

    private val saved = mutableListOf<ExternalBalanceLocal>()

    private val removedWallets = mutableListOf<Long>()

    private val persisted = mutableListOf<ExternalBalanceLocal>()

    private val committedAmounts = mutableListOf<BigInteger>()

    private val dao = object : ExternalBalanceDao by daoDelegate {
        override suspend fun runInTransaction(action: suspend () -> Unit) {
            action()
            committedAmounts += persisted.fold(BigInteger.ZERO) { sum, balance -> sum + balance.amount }
        }

        override suspend fun getExternalBalance(metaId: Long, chainId: String, assetId: Int, type: ExternalBalanceLocal.Type, subtype: String) =
            persisted.firstOrNull {
                it.metaId == metaId && it.chainId == chainId && it.assetId == assetId && it.type == type && it.subtype == subtype
            }

        override suspend fun insertExternalBalance(externalBalance: ExternalBalanceLocal) {
            saved += externalBalance
            persisted.removeAll {
                it.metaId == externalBalance.metaId && it.chainId == externalBalance.chainId && it.assetId == externalBalance.assetId &&
                    it.type == externalBalance.type && it.subtype == externalBalance.subtype
            }
            persisted += externalBalance
        }

        override suspend fun removeExternalBalance(metaId: Long, chainId: String, assetId: Int, type: ExternalBalanceLocal.Type, subtype: String?) {
            assertEquals(ChainGeneses.BITTENSOR, chainId)
            assertEquals(ExternalBalanceLocal.Type.BITTENSOR, type)
            removedWallets += metaId
            persisted.removeAll {
                it.metaId == metaId && it.chainId == chainId && it.assetId == assetId && it.type == type && it.subtype == subtype
            }
        }
    }

    private val updater = BittensorBalanceUpdater(scope, chain, positions, dao)

    private val coldKey = ByteArray(32)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val subnetId = BittensorNetuid.Subnet(1)

    private val holdings = BittensorPortfolioPositions(
        root = listOf(BittensorRootPosition(hotkey, BigInteger.TEN)),
        subnets = listOf(BittensorSubnetPosition(subnetId, hotkey, BigInteger.valueOf(1_000_000_000))),
        subnetPrices = mapOf(subnetId to BigInteger.valueOf(20))
    )

    @Test
    fun `complete root and alpha valuation is counted once and scoped to the wallet`() = runBlocking<Unit> {
        prepare(listOf(BittensorPortfolioState(holdings, BittensorDataStatus.Fresh)))

        updater.listenForUpdates(subscription, account).collect {}

        assertEquals(BigInteger.valueOf(30), saved.single().amount)
        assertEquals(7L, saved.single().metaId)
        assertEquals(ExternalBalanceLocal.Type.BITTENSOR, saved.single().type)
    }

    @Test
    fun `missing prices and stale positions do not replace a complete balance`() = runBlocking<Unit> {
        prepare(
            listOf(
                BittensorPortfolioState(holdings, BittensorDataStatus.Fresh),
                BittensorPortfolioState(holdings.copy(subnetPrices = emptyMap()), BittensorDataStatus.Fresh),
                BittensorPortfolioState(holdings, BittensorDataStatus.Stale(IllegalStateException())),
                BittensorPortfolioState(null, BittensorDataStatus.Loading)
            )
        )

        updater.listenForUpdates(subscription, account).collect {}

        assertEquals(1, saved.size)
        assertEquals(BigInteger.valueOf(30), persisted.single().amount)
    }

    @Test
    fun `fresh empty holdings remove the old balance`() = runBlocking<Unit> {
        val empty = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())
        prepare(listOf(BittensorPortfolioState(empty, BittensorDataStatus.Fresh)))

        updater.listenForUpdates(subscription, account).collect {}

        assertTrue(saved.isEmpty())
        assertEquals(listOf(7L, 7L), removedWallets)
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun `initial partial includes root and priced subnets without claiming a complete total`() = runBlocking<Unit> {
        val unpriced = BittensorSubnetPosition(BittensorNetuid.Subnet(2), hotkey, BigInteger.TEN)
        val partial = holdings.copy(subnets = holdings.subnets + unpriced)
        prepare(listOf(BittensorPortfolioState(partial, BittensorDataStatus.Fresh)))

        updater.listenForUpdates(subscription, account).collect {}

        assertEquals(BigInteger.valueOf(30), persisted.single().amount)
        assertEquals("partial", persisted.single().subtype)
        assertNull(partial.totalStake)
        assertEquals(false, partial.valuation.isComplete)
    }

    @Test
    fun `a new updater retains the persisted complete value on partial data`() = runBlocking<Unit> {
        prepare(listOf(BittensorPortfolioState(holdings, BittensorDataStatus.Fresh)))
        updater.listenForUpdates(subscription, account).collect {}
        val restartedUpdater = BittensorBalanceUpdater(scope, chain, positions, dao)
        val partial = holdings.copy(root = emptyList(), subnetPrices = emptyMap())
        prepare(listOf(BittensorPortfolioState(partial, BittensorDataStatus.Fresh)))

        restartedUpdater.listenForUpdates(subscription, account).collect {}

        assertEquals(1, saved.size)
        assertEquals(BigInteger.valueOf(30), persisted.single().amount)
        assertEquals(ExternalBalanceLocal.EMPTY_SUBTYPE, persisted.single().subtype)
    }

    @Test
    fun `complete valuation replaces partial once without double counting`() = runBlocking<Unit> {
        val partial = holdings.copy(subnetPrices = emptyMap())
        prepare(
            listOf(
                BittensorPortfolioState(partial, BittensorDataStatus.Fresh),
                BittensorPortfolioState(holdings, BittensorDataStatus.Fresh),
                BittensorPortfolioState(holdings, BittensorDataStatus.Fresh)
            )
        )

        updater.listenForUpdates(subscription, account).collect {}

        assertEquals(listOf(BigInteger.TEN, BigInteger.valueOf(30)), committedAmounts)
        assertEquals(2, saved.size)
        assertEquals(ExternalBalanceLocal.EMPTY_SUBTYPE, persisted.single().subtype)
    }

    @Test
    fun `partial zero clears previous partial without storing a fake complete zero`() = runBlocking<Unit> {
        val partial = holdings.copy(subnetPrices = emptyMap())
        val unknownOnly = partial.copy(root = emptyList())
        prepare(
            listOf(
                BittensorPortfolioState(partial, BittensorDataStatus.Fresh),
                BittensorPortfolioState(unknownOnly, BittensorDataStatus.Fresh)
            )
        )

        updater.listenForUpdates(subscription, account).collect {}

        assertTrue(persisted.isEmpty())
        assertEquals(1, saved.size)
        assertNull(unknownOnly.totalStake)
    }

    @Test
    fun `complete balance for one wallet does not suppress partial for another`() = runBlocking<Unit> {
        prepare(listOf(BittensorPortfolioState(holdings, BittensorDataStatus.Fresh)))
        updater.listenForUpdates(subscription, account).collect {}
        val otherAccount = mock(MetaAccount::class.java)
        val otherColdKey = ByteArray(32) { 2 }
        `when`(otherAccount.id).thenReturn(8L)
        `when`(otherAccount.accountIdIn(chain)).thenReturn(otherColdKey)
        val partial = holdings.copy(subnetPrices = emptyMap())
        val states = listOf(BittensorPortfolioState(partial, BittensorDataStatus.Fresh))
        `when`(positions.observePortfolio(chain.id, otherColdKey)).thenReturn(states.asFlow())

        updater.listenForUpdates(subscription, otherAccount).collect {}

        assertEquals(mapOf(7L to BigInteger.valueOf(30), 8L to BigInteger.TEN), persisted.associate { it.metaId to it.amount })
    }

    @Test
    fun `fresh zero clears an existing partial balance`() = runBlocking<Unit> {
        val partial = holdings.copy(subnetPrices = emptyMap())
        val empty = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())
        prepare(
            listOf(
                BittensorPortfolioState(partial, BittensorDataStatus.Fresh),
                BittensorPortfolioState(empty, BittensorDataStatus.Fresh)
            )
        )

        updater.listenForUpdates(subscription, account).collect {}

        assertTrue(persisted.isEmpty())
        assertEquals(listOf(BigInteger.TEN, BigInteger.ZERO), committedAmounts)
    }

    @Test
    fun `stale initial partial is not persisted`() = runBlocking<Unit> {
        val partial = holdings.copy(subnetPrices = emptyMap())
        prepare(listOf(BittensorPortfolioState(partial, BittensorDataStatus.Stale(IllegalStateException()))))

        updater.listenForUpdates(subscription, account).collect {}

        assertTrue(committedAmounts.isEmpty())
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun `upstream cancellation propagates without persisting a balance`() = runBlocking<Unit> {
        prepare(emptyList())
        val cancelled = flow<BittensorPortfolioState> { throw CancellationException("cancelled") }
        `when`(positions.observePortfolio(chain.id, coldKey)).thenReturn(cancelled)

        val failure = runCatching { updater.listenForUpdates(subscription, account).collect {} }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertEquals("cancelled", failure?.message)
        assertTrue(committedAmounts.isEmpty())
    }

    @Test
    fun `other chains do not start a Bittensor subscription`() = runBlocking<Unit> {
        `when`(chain.id).thenReturn("other")

        updater.listenForUpdates(subscription, account).collect {}

        verifyNoInteractions(positions)
    }

    private fun prepare(states: List<BittensorPortfolioState>) {
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(chainAsset.id).thenReturn(0)
        `when`(account.id).thenReturn(7L)
        `when`(account.accountIdIn(chain)).thenReturn(coldKey)
        `when`(positions.observePortfolio(chain.id, coldKey)).thenReturn(states.asFlow())
    }
}

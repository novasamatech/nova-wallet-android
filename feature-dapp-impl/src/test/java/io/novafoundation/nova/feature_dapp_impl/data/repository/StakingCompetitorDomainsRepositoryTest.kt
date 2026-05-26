package io.novafoundation.nova.feature_dapp_impl.data.repository

import io.novafoundation.nova.feature_dapp_impl.data.network.competitors.StakingCompetitorDomainsApi
import io.novafoundation.nova.feature_dapp_impl.data.network.competitors.StakingCompetitorsResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StakingCompetitorDomainsRepositoryTest {

    private class FakeApi(
        private val behavior: () -> StakingCompetitorsResponse
    ) : StakingCompetitorDomainsApi {
        override suspend fun getDomains(url: String): StakingCompetitorsResponse = behavior()
    }

    private fun repo(api: StakingCompetitorDomainsApi): RealStakingCompetitorDomainsRepository =
        RealStakingCompetitorDomainsRepository(api = api, remoteUrl = "https://stub.test/x.json")

    @Test
    fun emptyCacheReturnsFalseForAnyUrl() {
        val r = repo(FakeApi { throw IllegalStateException("never called") })

        assertFalse(r.isStakingCompetitor("https://staking.polkadot.cloud/"))
        assertFalse(r.isStakingCompetitor("https://app.hydration.net/"))
    }

    @Test
    fun successfulSyncPopulatesCacheAndMatchesExactHost() = runBlocking {
        val r = repo(FakeApi { StakingCompetitorsResponse(version = 1, domains = listOf("staking.polkadot.cloud")) })

        r.sync()

        assertTrue(r.isStakingCompetitor("https://staking.polkadot.cloud/"))
        assertTrue(r.isStakingCompetitor("https://staking.polkadot.cloud/validators"))
    }

    @Test
    fun successfulSyncMatchesSubdomain() = runBlocking {
        val r = repo(FakeApi { StakingCompetitorsResponse(version = 1, domains = listOf("staking.polkadot.cloud")) })

        r.sync()

        assertTrue(r.isStakingCompetitor("https://sub.staking.polkadot.cloud/"))
    }

    @Test
    fun successfulSyncDoesNotMatchUnrelatedHost() = runBlocking {
        val r = repo(FakeApi { StakingCompetitorsResponse(version = 1, domains = listOf("staking.polkadot.cloud")) })

        r.sync()

        assertFalse(r.isStakingCompetitor("https://app.hydration.net/"))
        assertFalse("apex should not match a subdomain-listed entry", r.isStakingCompetitor("https://polkadot.cloud/"))
    }

    @Test
    fun matchIsCaseInsensitive() = runBlocking {
        val r = repo(FakeApi { StakingCompetitorsResponse(version = 1, domains = listOf("Staking.Polkadot.Cloud")) })

        r.sync()

        assertTrue(r.isStakingCompetitor("https://staking.polkadot.cloud/"))
        assertTrue(r.isStakingCompetitor("https://STAKING.POLKADOT.CLOUD/"))
    }

    @Test
    fun networkFailureLeavesCacheEmpty_failOpen() = runBlocking {
        val r = repo(FakeApi { throw RuntimeException("boom") })

        r.sync()

        assertFalse(
            "Fail-open: network failure must NOT block navigation by returning true",
            r.isStakingCompetitor("https://staking.polkadot.cloud/")
        )
    }

    @Test
    fun malformedUrlReturnsFalse() = runBlocking {
        val r = repo(FakeApi { StakingCompetitorsResponse(version = 1, domains = listOf("staking.polkadot.cloud")) })

        r.sync()

        assertFalse(r.isStakingCompetitor("not a url at all"))
        assertFalse(r.isStakingCompetitor(""))
    }
}

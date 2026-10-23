package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorValidatorTimeoutTest {

    private val apiDelegate = mock(BittensorValidatorsApi::class.java)

    private val api = object : BittensorValidatorsApi by apiDelegate {
        override suspend fun validators(url: String): Response<BittensorValidatorsRemote> = awaitCancellation()

        override suspend fun recommendations(url: String): Response<BittensorRecommendationsRemote> = awaitCancellation()
    }

    private val urls = InfrastructureUrls { "https://infra.example/".toHttpUrl() }

    private val backend = BittensorValidatorBackend(api, urls)

    private val storage = mock(StorageDataSource::class.java)

    private val rpc = mock(RpcCalls::class.java)

    private val repository = RealBittensorSubnetValidatorRepository(backend, storage, rpc)

    private val chain = mock(Chain::class.java)

    @Test
    fun `owned directory timeout is recoverable not cancellation`() = runTest {
        val failure = runCatching { repository.rootValidators(chain) }.exceptionOrNull()
        assertTrue(failure is BittensorTimeoutException)
        assertFalse(failure is CancellationException)
    }

    @Test
    fun `owned eligibility timeout is recoverable not cancellation`() = runTest {
        val validator = BittensorValidator(AccountIdKey(ByteArray(32)), "Validator")
        val failure = runCatching { repository.validateRoot(chain, validator) }.exceptionOrNull()
        assertTrue(failure is BittensorTimeoutException)
        assertFalse(failure is CancellationException)
    }

    @Test
    fun `caller timeout remains cancellation during eligibility checks`() = runTest {
        val validator = BittensorValidator(AccountIdKey(ByteArray(32)), "Validator")
        val failure = runCatching {
            withTimeout(1000) { repository.validateRoot(chain, validator) }
        }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }

    @Test
    fun `caller timeout remains cancellation rather than a recoverable directory failure`() = runTest {
        val failure = runCatching {
            withTimeout(1000) { repository.directory(chain, BittensorNetuid.Root) }
        }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }
}

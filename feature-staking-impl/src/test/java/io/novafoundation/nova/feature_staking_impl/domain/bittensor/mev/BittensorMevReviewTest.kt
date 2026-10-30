package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Answers
import org.mockito.Mockito.mock
import java.io.IOException

class BittensorMevReviewTest {

    private val chain = mock(Chain::class.java)

    private val asset = mock(Chain.Asset::class.java)

    private val logger = mock(Logger::class.java)

    private val origin = SubmissionOrigin(ByteArray(32), ByteArray(32))

    private val clearFee = SubstrateFee(100.toBigInteger(), origin, asset)

    private val form: FormExtrinsicWithOrigin = {}

    private var attempts = 0

    private var cancel = false

    private val service = mock(BittensorMevService::class.java) { invocation ->
        when (invocation.method.name) {
            "supports" -> true
            "quote" -> {
                attempts += 1
                if (cancel) throw CancellationException("Screen closed")
                throw IOException("Quote RPC unavailable")
            }
            else -> Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val review = BittensorMevReview(service, logger)

    @Test
    fun `three failed quotes fall back for this review but a new order retries`() = runTest {
        val estimate = review.estimate(chain, 1, true, clearFee, form)
        assertEquals(3, attempts)
        assertFalse(estimate.isProtected)
        assertEquals(clearFee.amount, estimate.amount)
        review.estimate(chain, 1, true, clearFee, form)
        assertEquals(3, attempts)
        val nextOrder = BittensorMevReview(service, logger)
        nextOrder.estimate(chain, 1, true, clearFee, form)
        assertEquals(6, attempts)
    }

    @Test
    fun `screen cancellation propagates and does not become a clear fee fallback`() = runTest {
        cancel = true
        assertTrue(runCatching { review.estimate(chain, 1, true, clearFee, form) }.exceptionOrNull() is CancellationException)
        assertEquals(1, attempts)
    }
}

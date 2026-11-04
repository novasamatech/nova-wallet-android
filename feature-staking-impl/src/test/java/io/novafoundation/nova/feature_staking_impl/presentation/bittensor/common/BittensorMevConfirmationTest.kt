package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevAvailability
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevReviewFee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevUnavailableReason
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorMevConfirmationTest {

    private val asset = mock(Chain.Asset::class.java)

    private val origin = SubmissionOrigin(ByteArray(32), ByteArray(32))

    private val innerFee = SubstrateFee(100.toBigInteger(), origin, asset)

    private val carrierFee = SubstrateFee(25.toBigInteger(), origin, asset)

    private val quote = BittensorMevQuote(innerFee, carrierFee)

    @Test
    fun `enabling protection rejects the replayed clear fee before collectors catch up`() = runTest {
        checkSelectionChange(initiallyEnabled = false)
    }

    @Test
    fun `disabling protection rejects the replayed protected fee before collectors catch up`() = runTest {
        checkSelectionChange(initiallyEnabled = true)
    }

    @Test
    fun `a fee captured before a warning must still match when the warning resumes`() {
        val selected = MutableStateFlow(true)
        val confirmedFee = reviewFee(enabled = true)
        assertTrue(confirmedFee.matchesMevSelection(selected.value))

        selected.value = false

        assertFalse(confirmedFee.matchesMevSelection(selected.value))
    }

    @Test
    fun `unavailable protection preserves the reviewed clear fallback`() {
        BittensorMevUnavailableReason.entries.forEach { reason ->
            val availability = BittensorMevAvailability.Unavailable(reason)
            val fallbackFee = BittensorMevReviewFee(innerFee, requested = true, availability)

            assertFalse(fallbackFee.isProtected)
            assertTrue(fallbackFee.matchesMevSelection(enabled = true))
            assertFalse(fallbackFee.matchesMevSelection(enabled = false))
        }
    }

    @Test
    fun `ordinary root fees do not require a protection selection`() {
        assertTrue(innerFee.matchesMevSelection(enabled = true))
        assertTrue(innerFee.matchesMevSelection(enabled = false))
    }

    private suspend fun TestScope.checkSelectionChange(initiallyEnabled: Boolean) {
        val selected = MutableStateFlow(initiallyEnabled)
        val previousFee = reviewFee(initiallyEnabled)
        val loadedFee = MutableStateFlow(loaded(previousFee))
        val displayedFee = combine(loadedFee, selected) { fee, enabled -> fee.forMevSelection(enabled) }
            .shareIn(backgroundScope, SharingStarted.Eagerly, replay = 1)
        runCurrent()

        selected.value = !initiallyEnabled
        // Confirm can read the replay before the asynchronous presentation flow processes the toggle.
        val replayedFee = requireNotNull(displayedFee.first().loadedFeeOrNull()).fee
        assertSame(previousFee, replayedFee)
        assertFalse(replayedFee.matchesMevSelection(selected.value))

        runCurrent()
        assertNull(displayedFee.first().loadedFeeOrNull())

        val matchingFee = reviewFee(selected.value)
        loadedFee.value = loaded(matchingFee)
        runCurrent()
        val readyFee = requireNotNull(displayedFee.first().loadedFeeOrNull()).fee
        assertSame(matchingFee, readyFee)
        assertTrue(readyFee.matchesMevSelection(selected.value))
    }

    private fun reviewFee(enabled: Boolean) = BittensorMevReviewFee(
        clearFee = innerFee,
        requested = enabled,
        availability = if (enabled) BittensorMevAvailability.Available(quote) else BittensorMevAvailability.Disabled
    )

    private fun loaded(fee: Fee): FeeStatus<BittensorFeeEstimate<Unit>, Unit> = FeeStatus.Loaded(FeeModel(BittensorFeeEstimate(Unit, fee), Unit))
}

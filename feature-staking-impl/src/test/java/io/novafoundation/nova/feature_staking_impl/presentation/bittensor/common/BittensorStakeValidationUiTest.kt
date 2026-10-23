package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.notValidOrNull
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorStakeValidationUiTest {

    private val resources = mock(ResourceManager::class.java)

    private var refreshes = 0

    private var resumptions = 0

    private var revalidations = 0

    private val actions = object : ValidationFlowActions<BittensorStakePreflightPayload> {
        override fun resumeFlow(modifyPayload: ((BittensorStakePreflightPayload) -> BittensorStakePreflightPayload)?) {
            resumptions += 1
        }

        override fun revalidate(modifyPayload: ((BittensorStakePreflightPayload) -> BittensorStakePreflightPayload)?) {
            revalidations += 1
        }
    }

    @Before
    fun setup() {
        `when`(resources.getString(anyInt())).thenAnswer { it.arguments[0].toString() }
    }

    @Test
    fun `stale quote is only refreshed after explicit user action and does not resume submission`() {
        assertExplicitQuoteReview(BittensorStakeValidationFailure.QUOTE_EXPIRED)
    }

    @Test
    fun `price movement requires explicit quote review without automatic submission`() {
        assertExplicitQuoteReview(BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED)
    }

    @Test
    fun `inactive warning cancel stays put and continue repeats validations`() {
        val failure = validOrWarning<BittensorStakeValidationFailure>(false) { BittensorStakeValidationFailure.INACTIVE_VALIDATOR }
            .notValidOrNull()!!
        val transformed = bittensorStakeValidationFailureUi(
            failure,
            actions,
            resources,
            formatAmount = { "$it planks" }
        ) { refreshes += 1 } as TransformedFailure.Custom

        transformed.dialogPayload.cancelAction!!.action()
        assertEquals(0, revalidations)
        transformed.dialogPayload.okAction!!.action()
        assertEquals(1, revalidations)
        assertEquals(0, resumptions)
        assertEquals(0, refreshes)
    }

    @Test
    fun `accepting price impact repeats validations instead of resuming past them`() {
        val failure = validOrWarning<BittensorStakeValidationFailure>(false) { BittensorStakeValidationFailure.HIGH_PRICE_IMPACT }
            .notValidOrNull()!!
        val transformed = bittensorStakeValidationFailureUi(
            failure,
            actions,
            resources,
            formatAmount = { "$it planks" }
        ) { refreshes += 1 } as TransformedFailure.Custom

        transformed.dialogPayload.okAction!!.action()

        assertEquals(1, revalidations)
        assertEquals(0, resumptions)
        assertEquals(0, refreshes)
    }

    @Test
    fun `validation analytics reports only the chosen dialog outcome`() {
        val outcomes = mutableListOf<StakingValidationOutcome>()
        val failure = validOrError<BittensorStakeValidationFailure>(false) { BittensorStakeValidationFailure.QUOTE_EXPIRED }.notValidOrNull()!!
        val transformed = bittensorStakeValidationFailureUi(
            failure,
            actions,
            resources,
            formatAmount = { "$it planks" },
            onOutcome = { outcomes += it }
        ) { refreshes += 1 } as TransformedFailure.Custom
        assertEquals(emptyList<StakingValidationOutcome>(), outcomes)
        transformed.dialogPayload.cancelAction!!.action()
        assertEquals(listOf(StakingValidationOutcome.CANCELLED), outcomes)
        assertEquals(0, refreshes)
    }

    @Test
    fun `accepting an updated rate reports proceeded without submitting`() {
        val outcomes = mutableListOf<StakingValidationOutcome>()
        val failure = validOrError<BittensorStakeValidationFailure>(false) { BittensorStakeValidationFailure.QUOTE_EXPIRED }.notValidOrNull()!!
        val transformed = bittensorStakeValidationFailureUi(
            failure,
            actions,
            resources,
            formatAmount = { "$it planks" },
            onOutcome = { outcomes += it }
        ) { refreshes += 1 } as TransformedFailure.Custom
        transformed.dialogPayload.okAction!!.action()
        assertEquals(listOf(StakingValidationOutcome.PROCEEDED), outcomes)
        assertEquals(1, refreshes)
        assertEquals(0, resumptions)
    }

    @Test
    fun `minimum stake shows the iOS amount too low copy with the required amount`() {
        `when`(resources.getString(R.string.bittensor_validation_minimum_stake, "122 planks"))
            .thenReturn("You can't stake less than the network minimum value (122 planks)")
        val outcomes = mutableListOf<StakingValidationOutcome>()
        val failure = validOrError<BittensorStakeValidationFailure>(false) { BittensorStakeValidationFailure.BelowMinimumStake(122.toBigInteger()) }.notValidOrNull()!!
        val transformed = bittensorStakeValidationFailureUi(
            failure,
            actions,
            resources,
            formatAmount = { "$it planks" },
            onOutcome = { outcomes += it }
        ) { refreshes += 1 } as TransformedFailure.Default

        assertEquals(R.string.common_amount_low.toString(), transformed.titleAndMessage.first)
        assertEquals("You can't stake less than the network minimum value (122 planks)", transformed.titleAndMessage.second)
        assertEquals(listOf(StakingValidationOutcome.BLOCKED), outcomes)
    }

    @Test
    fun `other blocking failures keep the generic error title`() {
        val failure = BittensorStakeValidationFailure.FEE_RESERVE
        assertEquals(R.string.common_error_general_title.toString(), failure.title(resources))
        assertEquals(R.string.bittensor_validation_fee_reserve.toString(), failure.message(resources) { "unused" })
    }

    private fun assertExplicitQuoteReview(reason: BittensorStakeValidationFailure) {
        val failure = validOrError<BittensorStakeValidationFailure>(false) { reason }
            .notValidOrNull()!!
        val transformed = bittensorStakeValidationFailureUi(
            failure,
            actions,
            resources,
            formatAmount = { "$it planks" }
        ) { refreshes += 1 } as TransformedFailure.Custom

        assertEquals(0, refreshes)
        transformed.dialogPayload.cancelAction!!.action()
        assertEquals(0, refreshes)
        transformed.dialogPayload.okAction!!.action()
        assertEquals(1, refreshes)
        assertEquals(0, resumptions)
        assertEquals(0, revalidations)
    }
}

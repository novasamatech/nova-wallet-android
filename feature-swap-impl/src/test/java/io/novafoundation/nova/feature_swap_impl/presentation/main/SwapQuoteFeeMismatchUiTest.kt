package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_wallet_api.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class SwapQuoteFeeMismatchUiTest {

    private val resourceManager = mock(ResourceManager::class.java)

    private val validationActions = mock(ValidationFlowActions::class.java)

    @Test
    fun `retry reloads details without resuming or revalidating the obsolete payload`() {
        var reloads = 0
        val failure = transform { reloads++ }
        assertEquals(0, reloads)
        failure.dialogPayload.okAction!!.action()

        assertEquals(1, reloads)
        verifyNoInteractions(validationActions)
    }

    @Test
    fun `cancel neither reloads nor authorizes the obsolete payload`() {
        var reloads = 0
        val failure = transform { reloads++ }
        failure.dialogPayload.cancelAction!!.action()

        assertEquals(0, reloads)
        verifyNoInteractions(validationActions)
    }

    private fun transform(reload: () -> Unit): TransformedFailure.Custom {
        `when`(resourceManager.getString(R.string.swap_quote_fee_mismatch_title)).thenReturn("Swap details changed")
        `when`(resourceManager.getString(R.string.swap_quote_fee_mismatch_message)).thenReturn("Review refreshed details")
        `when`(resourceManager.getString(R.string.common_retry)).thenReturn("Retry")
        `when`(resourceManager.getString(R.string.common_cancel)).thenReturn("Cancel")
        return mapSwapValidationFailureToUI(
            resourceManager = resourceManager,
            status = SwapValidationFailure.QuoteFeeMismatch.validationError(),
            actions = validationActions,
            amountInSwapMaxAction = { error("Must not change the amount") },
            amountOutSwapMinAction = { _, _ -> error("Must not change the amount") },
            amountInSetAction = { _, _ -> error("Must not change the amount") },
            quoteFeeMismatchAction = reload
        ) as TransformedFailure.Custom
    }
}

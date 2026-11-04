package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/** Per-review quote fallback; it is never persisted or shared between orders. */
class BittensorMevReview(private val service: BittensorMevService, private val logger: Logger) {

    private var fallback = false

    suspend fun supports(metaId: Long): Boolean = service.supports(metaId)

    suspend fun estimate(
        chain: Chain,
        metaId: Long,
        enabled: Boolean,
        clearFee: Fee,
        form: FormExtrinsicWithOrigin
    ): BittensorMevReviewFee {
        if (!supports(metaId)) {
            return BittensorMevReviewFee(
                clearFee,
                enabled,
                BittensorMevAvailability.Unavailable(BittensorMevUnavailableReason.UNSUPPORTED_WALLET)
            )
        }
        if (fallback) return BittensorMevReviewFee(clearFee, enabled, BittensorMevAvailability.Unavailable(BittensorMevUnavailableReason.QUOTES_FAILED))
        if (!enabled) return BittensorMevReviewFee(clearFee, false, BittensorMevAvailability.Disabled)
        repeat(3) {
            try {
                val quote = service.quote(chain, metaId, form)
                return BittensorMevReviewFee(clearFee, enabled, quote)
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                logger.log("[BittensorMev] Shield quote failed")
                logger.log(e)
                if (it < 2) service.waitBeforeQuoteRetry(chain)
            }
        }
        fallback = true
        return BittensorMevReviewFee(clearFee, enabled, BittensorMevAvailability.Unavailable(BittensorMevUnavailableReason.QUOTES_FAILED))
    }
}

class BittensorMevReviewFee(
    clearFee: Fee,
    val requested: Boolean,
    val availability: BittensorMevAvailability
) : Fee by (if (requested && availability is BittensorMevAvailability.Available) availability.quote.totalFee else clearFee) {

    val quote: BittensorMevQuote? get() = (availability as? BittensorMevAvailability.Available)?.quote

    val isProtected: Boolean get() = requested && availability is BittensorMevAvailability.Available
}

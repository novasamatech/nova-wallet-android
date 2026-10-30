package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import kotlinx.coroutines.TimeoutCancellationException
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevExtrinsics
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevProtocol
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevSealer
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevSource
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.extensions.fromHex
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withTimeout
import java.math.BigInteger

class BittensorMevService(
    private val accounts: AccountRepository,
    private val signers: SignerProvider,
    private val source: BittensorMevSource,
    private val extrinsics: BittensorMevExtrinsics,
    private val sealer: BittensorMevSealer,
    private val tracker: BittensorMevTracker,
    private val chainState: ChainStateRepository,
    private val logger: Logger
) {

    suspend fun supports(metaId: Long): Boolean = accounts.getMetaAccount(metaId).type == LightMetaAccount.Type.SECRETS

    suspend fun feeOrClear(chain: Chain, metaId: Long, clearFee: Fee, form: FormExtrinsicWithOrigin): Fee {
        return try {
            when (val availability = quote(chain, metaId, form)) {
                is BittensorMevAvailability.Available -> availability.quote.totalFee
                else -> clearFee
            }
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            logger.log("[BittensorMev] Setup shield quote unavailable; using the clear fee")
            logger.log(e)
            clearFee
        }
    }

    suspend fun waitBeforeQuoteRetry(chain: Chain) {
        delay(chainState.expectedBlockTimeInMillis(chain.id).toLong().coerceAtLeast(1))
    }

    suspend fun quote(chain: Chain, metaId: Long, form: FormExtrinsicWithOrigin): BittensorMevAvailability {
        if (!supports(metaId)) return BittensorMevAvailability.Unavailable(BittensorMevUnavailableReason.UNSUPPORTED_WALLET)
        val blockMillis = chainState.expectedBlockTimeInMillis(chain.id).toLong().coerceAtLeast(1)
        return withTimeout(blockMillis * BittensorMevProtocol.READ_TIMEOUT_BLOCKS) {
            if (source.nextKey(chain) == null) return@withTimeout BittensorMevAvailability.Unavailable(BittensorMevUnavailableReason.KEY_UNAVAILABLE)
            val availability = estimate(chain, metaId, form)
            if (availability !is BittensorMevAvailability.Available) return@withTimeout availability
            val account = accounts.getMetaAccount(metaId)
            val funds = source.funds(chain, account.requireAccountIdIn(chain))
            if (funds.canPay(availability.quote)) {
                availability
            } else {
                BittensorMevAvailability.Unavailable(
                    BittensorMevUnavailableReason.INSUFFICIENT_NATIVE_BALANCE
                )
            }
        }
    }

    suspend fun submit(
        chain: Chain,
        metaId: Long,
        form: FormExtrinsicWithOrigin,
        onUnconfirmed: (String) -> Unit = {}
    ): BittensorMevExecution {
        check(supports(metaId))
        val account = accounts.getMetaAccount(metaId)
        val accountId = account.requireAccountIdIn(chain)
        val signer = signers.rootSignerFor(account)
        val blockMillis = chainState.expectedBlockTimeInMillis(chain.id).toLong().coerceAtLeast(1)
        // Readiness precedes the PIN prompt; no ordinary submission is allowed after this choice was confirmed.
        withTimeoutOrNull(blockMillis * 6) {
            val availability = estimate(chain, metaId, form) as? BittensorMevAvailability.Available ?: throw BittensorMevNotReady()
            val quote = availability.quote
            if (!source.funds(chain, accountId).canPay(quote)) throw BittensorMevNotReady()
            var key = source.nextKey(chain)
            repeat(2) {
                if (key == null) {
                    delay(blockMillis)
                    key = source.nextKey(chain)
                }
            }
            if (key == null) throw BittensorMevNotReady()
            true
        } ?: throw BittensorMevNotReady()
        try {
            signer.preAuthorizeSignatures(2)
            val built = withTimeoutOrNull(blockMillis * BittensorMevProtocol.READ_TIMEOUT_BLOCKS) {
                val timing = source.timing(chain, chain.addressOf(accountId))
                val inner = extrinsics.build(chain, signer, timing, timing.nonce + BigInteger.ONE, false, form)
                // NextKey rotates every block and opens two blocks later. Never reuse the quote/readiness key.
                val key = source.nextKey(chain) ?: throw BittensorMevNotReady()
                val ciphertext = sealer.seal(inner.extrinsicHex.fromHex(), key)
                val carrier = extrinsics.build(chain, signer, timing, timing.nonce, false, extrinsics.carrier(ciphertext))
                PreparedShieldedOrder(timing, inner, carrier)
            } ?: throw BittensorMevNotReady()
            val hierarchy = signer.getSigningHierarchy()
            return try {
                tracker.submitAndTrack(chain, built.timing, built.inner, built.carrier, hierarchy, blockMillis, onUnconfirmed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Once handed to RPC, an error never proves that the carrier was not broadcast.
                BittensorMevExecution.Unconfirmed(built.inner.extrinsicHex.extrinsicHash())
            }
        } finally {
            signer.clearSigningAuthorization()
        }
    }

    private suspend fun estimate(chain: Chain, metaId: Long, form: FormExtrinsicWithOrigin): BittensorMevAvailability {
        val account = accounts.getMetaAccount(metaId)
        val signer = signers.rootSignerFor(account)
        val timing = source.timing(chain, chain.addressOf(account.requireAccountIdIn(chain)))
        val inner = extrinsics.build(chain, signer, timing, timing.nonce + BigInteger.ONE, true, form)
        val payloadSize = BittensorMevProtocol.payloadSize(inner.extrinsicHex.fromHex().size)
        if (payloadSize > BittensorMevProtocol.MAX_PAYLOAD_BYTES) return BittensorMevAvailability.Unavailable(BittensorMevUnavailableReason.PAYLOAD_TOO_LARGE)
        val carrier = extrinsics.build(chain, signer, timing, timing.nonce, true, extrinsics.carrier(ByteArray(payloadSize)))
        val innerFee = extrinsics.fee(chain, signer, inner)
        val carrierFee = extrinsics.fee(chain, signer, carrier)
        return BittensorMevAvailability.Available(BittensorMevQuote(innerFee, carrierFee))
    }
}

private class PreparedShieldedOrder(
    val timing: BittensorMevTiming,
    val inner: SendableExtrinsic,
    val carrier: SendableExtrinsic
)

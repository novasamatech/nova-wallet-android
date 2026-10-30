package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_staking_impl.data.bittensor.bindBittensorOperationOutcome
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitStatus
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import io.novafoundation.nova.common.base.errors.SigningCancelledException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Nonce, mortality and signing with a key held in the app: past this the node is stalled, and nothing has been sent yet.
internal val BITTENSOR_BUILD_AND_SIGN_TIMEOUT = 60.seconds

// author_submitAndWatchExtrinsic until InBlock plus its events: past this the outcome is unknown, never retried.
internal val BITTENSOR_SEND_AND_INCLUSION_TIMEOUT = 3.minutes

/** Building or signing did not finish in time, so the extrinsic was never sent. */
class BittensorBuildTimeoutException : Exception("Building the Bittensor transaction timed out")

class BittensorSubmissionExecutor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
    private val positionUpdates: BittensorPositionUpdates,
    private val outcomeResolver: BittensorExtrinsicOutcomeResolver
) {

    private val mutex = Mutex()

    suspend fun execute(
        metaId: Long,
        chain: Chain,
        target: BittensorOutcomeTarget? = null,
        onStage: (String) -> Unit = {},
        formExtrinsic: FormExtrinsicWithOrigin
    ): BittensorOperationResult = mutex.withLock {
        try {
            onStage("Submission lock acquired; building and signing")
            val watch = buildAndSign(metaId, chain, formExtrinsic)
                ?: return@withLock BittensorBuildTimeoutException().mapBittensorPreSubmissionError()

            var lastSeenHash: String? = null
            onStage("Build/sign finished; collecting submission watch")
            val execution = withTimeoutOrNull(BITTENSOR_SEND_AND_INCLUSION_TIMEOUT) {
                val inBlock = watch.onEach {
                    lastSeenHash = it.status.extrinsicHash
                    onStage("Watch status=${it.status.javaClass.simpleName}; hash=$lastSeenHash")
                }.awaitStatus<ExtrinsicStatus.InBlock>()
                onStage("Included in block; resolving dispatch events")
                outcomeResolver.resolve(chain, inBlock)
            } ?: run {
                onStage("Inclusion/events timeout; outcome unknown; hash=$lastSeenHash")
                return@withLock BittensorOperationResult.Unknown(lastSeenHash)
            }

            onStage("Dispatch resolved; mapping result")

            val result = execution.mapBittensorOperationResult(chain)
            val coldkey = execution.submissionHierarchy.path.lastOrNull()?.account
                ?.takeIf { it.id == metaId }?.accountIdIn(chain)?.let(::AccountIdKey)
            val payer = execution.submissionHierarchy.path.firstOrNull()?.account?.accountIdIn(chain)?.let(::AccountIdKey)
            val events = when (val dispatch = execution.outcome) {
                is ExtrinsicDispatch.Ok -> dispatch.emittedEvents
                is ExtrinsicDispatch.Failed -> dispatch.emittedEvents
                ExtrinsicDispatch.Unknown -> emptyList()
            }
            val outcome = if (target != null && coldkey != null) {
                bindBittensorOperationOutcome(events, target, coldkey, payer, execution.blockHash)
            } else {
                null
            }
            if (result is BittensorOperationResult.Success) positionUpdates.confirmed(metaId, chain.id, coldkey)
            when (result) {
                is BittensorOperationResult.Success -> result.copy(outcome = outcome)
                // Failed calls may still have paid fees; never expose their trade events as executed amounts.
                is BittensorOperationResult.Failure -> result.copy(outcome = outcome?.copy(tao = null, alpha = null, serviceFee = null))
                is BittensorOperationResult.Unknown -> result
            }
        } catch (e: SigningCancelledException) {
            e.mapBittensorPreSubmissionError()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.mapBittensorSubmissionError()
        }
    }

    /**
     * Returns the cold watch flow once the extrinsic is built and signed (collecting it sends), or null on
     * [BITTENSOR_BUILD_AND_SIGN_TIMEOUT].
     *
     * Only a key held in the app is bounded here (SECRETS, or PROXIED whose proxy chain ends in one). Wallets that sign
     * outside the app, [SIGNED_OUTSIDE_APP] and PROXIED through one of them, have an unbounded build phase: the person
     * signing controls that time and can cancel it on the signing screen. Deviation: a node stall during their nonce or
     * mortality fetch, before the signing screen opens, is not cut by any timeout, because the
     * [BITTENSOR_SEND_AND_INCLUSION_TIMEOUT] phase starts only after signing.
     */
    private suspend fun buildAndSign(
        metaId: Long,
        chain: Chain,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Flow<ExtrinsicWatchResult<ExtrinsicStatus>>? {
        val bounded = !signsOutsideApp(metaId)
        // submitAndWatchExtrinsic returns failures as Result instead of throwing, so a failed build does not cancel this
        // coroutineScope and getOrThrow below rethrows the original exception.
        val built = coroutineScope {
            val build = async {
                val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)
                extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.WalletWithId(metaId), options, formExtrinsic)
            }

            if (bounded) {
                withTimeoutOrNull(BITTENSOR_BUILD_AND_SIGN_TIMEOUT) { build.await() } ?: run {
                    build.cancel()
                    null
                }
            } else {
                build.await()
            }
        }

        return built?.getOrThrow()
    }

    private suspend fun signsOutsideApp(metaId: Long, depth: Int = 0): Boolean {
        val wallet = accountRepository.getMetaAccount(metaId)
        return if (wallet is ProxiedMetaAccount && depth < MAX_PROXY_DEPTH) {
            signsOutsideApp(wallet.proxy.proxyMetaId, depth + 1)
        } else {
            wallet.type in SIGNED_OUTSIDE_APP
        }
    }

    private companion object {

        // Signed on another device or by another account: the signing time belongs to the person, not the node.
        val SIGNED_OUTSIDE_APP = setOf(
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.POLKADOT_VAULT,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.LEDGER_LEGACY,
            LightMetaAccount.Type.MULTISIG
        )

        const val MAX_PROXY_DEPTH = 4
    }
}

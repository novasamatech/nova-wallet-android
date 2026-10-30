package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.bindDispatchError
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findExtrinsicFailureOrThrow
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Reads the dispatch outcome of an included extrinsic. Same rules as `submitExtrinsicAndAwaitExecution`; it exists
 * because [BittensorSubmissionExecutor] watches the submission itself to bound build and inclusion separately.
 */
fun interface BittensorExtrinsicOutcomeResolver {

    suspend fun resolve(chain: Chain, inBlock: ExtrinsicWatchResult<ExtrinsicStatus.InBlock>): ExtrinsicExecutionResult
}

class RealBittensorExtrinsicOutcomeResolver(
    private val eventsRepository: EventsRepository,
    private val chainRegistry: ChainRegistry
) : BittensorExtrinsicOutcomeResolver {

    override suspend fun resolve(chain: Chain, inBlock: ExtrinsicWatchResult<ExtrinsicStatus.InBlock>): ExtrinsicExecutionResult {
        val status = inBlock.status
        val outcome = runCatching {
            val extrinsic = requireNotNull(eventsRepository.getExtrinsicWithEvents(chain.id, status.extrinsicHash, status.blockHash)) {
                "No extrinsic included into expected block"
            }
            if (extrinsic.isSuccess()) {
                ExtrinsicDispatch.Ok(extrinsic.events)
            } else {
                val runtime = chainRegistry.getRuntime(chain.id)
                val error = runtime.provideContext { bindDispatchError(extrinsic.events.findExtrinsicFailureOrThrow().arguments.first()) }
                ExtrinsicDispatch.Failed(error, extrinsic.events)
            }
        }.getOrElse {
            if (it is CancellationException) throw it
            Log.w(LOG_TAG, "Failed to determine Bittensor extrinsic outcome", it)
            ExtrinsicDispatch.Unknown
        }

        return ExtrinsicExecutionResult(status.extrinsicHash, status.blockHash, outcome, inBlock.submissionHierarchy)
    }
}

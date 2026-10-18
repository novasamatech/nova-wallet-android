package io.novafoundation.nova.feature_account_impl.data.extrinsic.recovery

import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.status
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.DefaultSignedExtensions
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Era
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.findExplicitOrNull
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import java.math.BigInteger

interface ExtrinsicRecoverySource {

    suspend fun finalizedBlock(chainId: String): FinalizedBlockReference

    suspend fun blockHash(chainId: String, number: BigInteger): String

    /** Null means absent; a found transaction without readable dispatch events throws. */
    suspend fun outcome(chainId: String, blockHash: String, transactionHash: String): RecoveredExtrinsicOutcome?

    suspend fun expiry(chainId: String, extrinsic: SendableExtrinsic): BigInteger
}

class RpcExtrinsicRecoverySource(
    private val rpcCalls: RpcCalls,
    private val eventsRepository: EventsRepository
) : ExtrinsicRecoverySource {

    override suspend fun finalizedBlock(chainId: String): FinalizedBlockReference {
        val hash = rpcCalls.getFinalizedHead(chainId)
        val header = rpcCalls.getBlockHeader(chainId, hash)
        return FinalizedBlockReference(BigInteger.valueOf(header.number.toLong()), hash)
    }

    override suspend fun blockHash(chainId: String, number: BigInteger): String = rpcCalls.getBlockHash(chainId, number)

    override suspend fun outcome(chainId: String, blockHash: String, transactionHash: String): RecoveredExtrinsicOutcome? {
        val block = rpcCalls.getBlock(chainId, blockHash)
        val containsTransaction = block.block.extrinsics.any {
            it.extrinsicHash().equals(transactionHash, ignoreCase = true)
        }

        if (!containsTransaction) {
            return null
        }
        // Check the raw hash first: undecodable or pruned events must never look like absence.
        val extrinsic = requireNotNull(eventsRepository.getExtrinsicWithEvents(chainId, transactionHash, blockHash))
        return recoveredExtrinsicDispatch(
            extrinsic.extrinsic.call,
            requireNotNull(extrinsic.status()) { "Transaction dispatch events are unavailable" },
            extrinsic.events
        )
    }

    override suspend fun expiry(chainId: String, extrinsic: SendableExtrinsic): BigInteger {
        val era = extrinsic.extrinsic.findExplicitOrNull(DefaultSignedExtensions.CHECK_MORTALITY)
        require(era is Era.Mortal) { "Extrinsic recovery requires a mortal transaction" }
        // Signing may span an era boundary. A later reference can only extend our scan window.
        return conservativeExtrinsicExpiry(era, finalizedBlock(chainId).number)
    }
}

internal fun conservativeExtrinsicExpiry(era: Era.Mortal, reference: BigInteger): BigInteger {
    require(era.period in 4..65536 && era.period.countOneBits() == 1 && era.phase in 0 until era.period)
    require(reference.isNonNegative)

    val period = BigInteger.valueOf(era.period.toLong())
    val phase = BigInteger.valueOf(era.phase.toLong())
    return (reference.max(phase) - phase)
        .divide(period)
        .multiply(period) + phase + period
}

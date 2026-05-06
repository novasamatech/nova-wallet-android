package io.novafoundation.nova.feature_staking_impl.data.subtensor.network

import io.novafoundation.nova.feature_staking_impl.data.subtensor.decoder.SubtensorStakeInfoDecoder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novafoundation.nova.runtime.network.rpc.stateCall
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.state.StateCallRequest
import java.math.BigInteger

/**
 * Resolved (hotkey, netuid, amount) tuple for a coldkey position. The runtime
 * pre-aggregates root + every subnet + V1/V2 alpha storage, so this is the
 * canonical view of the user's stake.
 */
data class SubtensorRawPosition(
    val hotkey: ByteArray,
    val netuid: Int,
    val amount: BigInteger,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubtensorRawPosition) return false
        return hotkey.contentEquals(other.hotkey) && netuid == other.netuid && amount == other.amount
    }

    override fun hashCode(): Int {
        var result = hotkey.contentHashCode()
        result = 31 * result + netuid
        result = 31 * result + amount.hashCode()
        return result
    }
}

/**
 * Fetches all non-zero stake positions for `coldkey` via Bittensor's
 * `StakeInfoRuntimeApi_get_stake_info_for_coldkey` runtime API. The runtime
 * resolves V1/V2 alpha + root accounting and returns one entry per
 * (hotkey, netuid). Mirrors `SubtensorPositionFetcher.swift` on iOS.
 *
 * Walking storage maps directly was abandoned because it silently dropped
 * root TAO positions (root validators aren't always inserted into
 * `StakingHotkeys`) — see CLAUDE_tao_staking.md for the full history.
 */
class SubtensorPositionFetcher(
    private val chainRegistry: ChainRegistry,
) {

    suspend fun fetchPositions(chainId: ChainId, coldkey: ByteArray): List<SubtensorRawPosition> {
        val socket = chainRegistry.getSocket(chainId)
        val coldkeyHex = "0x" + coldkey.toHexString(withPrefix = false)

        val request = StateCallRequest(RUNTIME_API, coldkeyHex)

        val responseHex = socket.stateCall(request) ?: return emptyList()

        return SubtensorStakeInfoDecoder.decode(responseHex)
            .filter { it.stake.signum() > 0 }
            .map { SubtensorRawPosition(hotkey = it.hotkey, netuid = it.netuid, amount = it.stake) }
    }

    companion object {
        private const val RUNTIME_API = "StakeInfoRuntimeApi_get_stake_info_for_coldkey"
    }
}

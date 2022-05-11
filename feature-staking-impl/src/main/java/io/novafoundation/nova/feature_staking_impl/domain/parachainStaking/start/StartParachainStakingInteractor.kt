package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.delegate
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.FixedByteArray
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.skipAliases
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import java.math.BigInteger

interface StartParachainStakingInteractor {

    suspend fun estimateFee(amount: BigInteger): BigInteger
}

class RealStartParachainStakingInteractor(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val singleAssetSharedState: SingleAssetSharedState,
): StartParachainStakingInteractor {

    override suspend fun estimateFee(amount: BigInteger): BigInteger {
       val chain = singleAssetSharedState.chain()

        return extrinsicService.estimateFee(chain) {
            delegate(
                candidate = fakeCollatorId(chain.id),
                amount = amount,
                candidateDelegationCount = fakeDelegationCount(),
                delegationCount = fakeDelegationCount()
            )
        }
    }

    private fun fakeDelegationCount() = BigInteger.TEN

    private suspend fun fakeCollatorId(chainId: ChainId): AccountId {
        val runtime = chainRegistry.getRuntime(chainId)
        val callMetadata = runtime.metadata.parachainStaking().call("delegate")
        val candidateIdType = callMetadata.arguments.first().type?.skipAliases()!!
        require(candidateIdType is FixedByteArray) {
            "accountId is not FixedByteArray but ${candidateIdType::class.simpleName}"
        }

        return ByteArray(candidateIdType.length)
    }
}

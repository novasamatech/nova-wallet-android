package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.delegate
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.FixedByteArray
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.skipAliases
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigInteger

interface StartParachainStakingInteractor {

    suspend fun estimateFee(amount: BigInteger): BigInteger

    suspend fun delegate(originAddress: String, amount: BigInteger, collator: AccountId): Result<*>

    // TODO stub until select collator screen is implemented
    suspend fun randomCollator(): Collator

    suspend fun getCollatorById(collatorId: AccountId): Collator

    suspend fun defaultMinimumStake(): BigInteger
}

class RealStartParachainStakingInteractor(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val collatorProvider: CollatorProvider,
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
    private val delegatorStateRepository: DelegatorStateRepository,
    private val candidatesRepository: CandidatesRepository,
) : StartParachainStakingInteractor {

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

    override suspend fun delegate(originAddress: String, amount: BigInteger, collator: AccountId) = withContext(Dispatchers.Default) {
        runCatching {
            val (chain, chainAsset) = singleAssetSharedState.chainAndAsset()
            val accountId = chain.accountIdOf(originAddress)

            val delegationsCount = when(val currentDelegationState = delegatorStateRepository.getDelegationState(chain, chainAsset, accountId)) {
                is DelegatorState.Delegator -> currentDelegationState.delegations.size
                is DelegatorState.None -> 0
            }

            val candidateMetadata = candidatesRepository.getCandidateMetadata(chain.id, collator)

            extrinsicService.submitAndWatchExtrinsic(chain, accountId) {
                delegate(
                    candidate = collator,
                    amount = amount,
                    candidateDelegationCount = candidateMetadata.delegationCount,
                    delegationCount = delegationsCount.toBigInteger()
                )
            }
                .filterIsInstance<ExtrinsicStatus.InBlock>()
                .first()
        }
    }

    override suspend fun randomCollator(): Collator = withContext(Dispatchers.Default) {
        val chainId = singleAssetSharedState.chainId()

        collatorProvider.electedCollators(chainId).first { it.accountIdHex == "bc9ccd0a3f84f47452b882d3f0796bb7e5be9ba0" }
    }

    override suspend fun getCollatorById(collatorId: AccountId): Collator {
        val chainId = singleAssetSharedState.chainId()

        return collatorProvider.electedCollator(chainId, collatorId)!!
    }

    override suspend fun defaultMinimumStake(): BigInteger {
        return stakingConstantsRepository.systemForcedMinStake(singleAssetSharedState.chainId())
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

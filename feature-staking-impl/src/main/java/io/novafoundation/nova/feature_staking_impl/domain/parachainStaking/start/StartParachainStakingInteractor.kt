package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationsCount
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.hasDelegation
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.delegate
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.delegatorBondMore
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
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

    suspend fun estimateFee(amount: BigInteger, collatorId: AccountId?): BigInteger

    suspend fun delegate(amount: BigInteger, collator: AccountId): Result<*>

    suspend fun checkDelegationsLimit(delegatorState: DelegatorState): DelegationsLimit
}

class RealStartParachainStakingInteractor(
    private val accountRepository: AccountRepository,
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val singleAssetSharedState: AnySelectedAssetOptionSharedState,
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
    private val delegatorStateRepository: DelegatorStateRepository,
    private val candidatesRepository: CandidatesRepository,
) : StartParachainStakingInteractor {

    override suspend fun estimateFee(amount: BigInteger, collatorId: AccountId?): BigInteger {
        val (chain, chainAsset) = singleAssetSharedState.chainAndAsset()
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = metaAccount.accountIdIn(chain)!!

        val currentDelegationState = delegatorStateRepository.getDelegationState(chain, chainAsset, accountId)

        return extrinsicService.estimateFee(chain) {
            if (collatorId != null && currentDelegationState.hasDelegation(collatorId)) {
                delegatorBondMore(
                    candidate = collatorId,
                    amount = amount
                )
            } else {
                delegate(
                    candidate = collatorId ?: fakeCollatorId(chain.id),
                    amount = amount,
                    candidateDelegationCount = fakeDelegationCount(),
                    delegationCount = fakeDelegationCount()
                )
            }
        }
    }

    override suspend fun delegate(amount: BigInteger, collator: AccountId) = withContext(Dispatchers.Default) {
        runCatching {
            val (chain, chainAsset) = singleAssetSharedState.chainAndAsset()
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val accountId = metaAccount.accountIdIn(chain)!!

            val currentDelegationState = delegatorStateRepository.getDelegationState(chain, chainAsset, accountId)

            extrinsicService.submitAndWatchExtrinsicAnySuitableWallet(chain, accountId) {
                if (currentDelegationState.hasDelegation(collator)) {
                    delegatorBondMore(
                        candidate = collator,
                        amount = amount
                    )
                } else {
                    val candidateMetadata = candidatesRepository.getCandidateMetadata(chain.id, collator)

                    delegate(
                        candidate = collator,
                        amount = amount,
                        candidateDelegationCount = candidateMetadata.delegationCount,
                        delegationCount = currentDelegationState.delegationsCount.toBigInteger()
                    )
                }
            }
                .filterIsInstance<ExtrinsicStatus.InBlock>()
                .first()
        }
    }

    override suspend fun checkDelegationsLimit(delegatorState: DelegatorState): DelegationsLimit {
        val maxDelegations = stakingConstantsRepository.maxDelegationsPerDelegator(delegatorState.chain.id).toInt()

        return if (delegatorState.delegationsCount < maxDelegations) {
            DelegationsLimit.NotReached
        } else {
            DelegationsLimit.Reached(maxDelegations)
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

package io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.data.source.trackLocksFlowOrEmpty
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.UnlockChunk
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimTime
import io.novafoundation.nova.feature_governance_api.domain.locks.RealClaimScheduleCalculator
import io.novafoundation.nova.feature_governance_api.domain.locks.claimableChunk
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceUnlockAffects.RemainsLockedInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.maxLockReplacing
import io.novafoundation.nova.feature_wallet_api.domain.model.transferableReplacingFrozen
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.selectedOption
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import io.novafoundation.nova.runtime.util.timerUntil
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface GovernanceUnlockInteractor {

    suspend fun calculateFee(claimable: UnlockChunk.Claimable?): Fee

    suspend fun unlock(claimable: UnlockChunk.Claimable?): Result<ExtrinsicStatus.InBlock>

    fun locksOverviewFlow(scope: CoroutineScope): Flow<GovernanceLocksOverview>

    fun unlockAffectsFlow(scope: CoroutineScope, assetFlow: Flow<Asset>): Flow<GovernanceUnlockAffects>
}

private class IntermediateData(
    val voting: Map<TrackId, Voting>,
    val currentBlockNumber: BlockNumber,
    val onChainReferenda: Map<ReferendumId, OnChainReferendum>,
    val durationEstimator: BlockDurationEstimator,
)

private const val LOCKS_OVERVIEW_KEY = "RealGovernanceUnlockInteractor.LOCKS_OVERVIEW_KEY"

class RealGovernanceUnlockInteractor(
    private val selectedAssetState: GovernanceSharedState,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val computationalCache: ComputationalCache,
    private val accountRepository: AccountRepository,
    private val balanceLocksRepository: BalanceLocksRepository,
    private val extrinsicService: ExtrinsicService,
) : GovernanceUnlockInteractor {

    override suspend fun calculateFee(claimable: UnlockChunk.Claimable?): Fee {
        val governanceSelectedOption = selectedAssetState.selectedOption()
        val chain = governanceSelectedOption.assetWithChain.chain

        if (claimable == null) return extrinsicService.zeroFee(chain, TransactionOrigin.SelectedWallet)

        val metaAccount = accountRepository.getSelectedMetaAccount()
        val origin = metaAccount.accountIdIn(chain)!!

        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            executeUnlock(origin, governanceSelectedOption, claimable)
        }
    }

    override suspend fun unlock(claimable: UnlockChunk.Claimable?) = withContext(Dispatchers.Default) {
        val governanceSelectedOption = selectedAssetState.selectedOption()
        val chain = governanceSelectedOption.assetWithChain.chain

        extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) { origin ->
            if (claimable == null) error("Nothing to claim")

            executeUnlock(accountIdToUnlock = origin.requestedOrigin, governanceSelectedOption, claimable)
        }.awaitInBlock()
    }

    private suspend fun ExtrinsicBuilder.executeUnlock(
        accountIdToUnlock: AccountId,
        selectedGovernanceOption: SupportedGovernanceOption,
        claimable: UnlockChunk.Claimable
    ) {
        val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)

        with(governanceSource.convictionVoting) {
            unlock(accountIdToUnlock, claimable)
        }
    }

    override fun locksOverviewFlow(scope: CoroutineScope): Flow<GovernanceLocksOverview> {
        return computationalCache.useSharedFlow(LOCKS_OVERVIEW_KEY, scope) {
            val governanceSelectedOption = selectedAssetState.selectedOption()

            val metaAccount = accountRepository.getSelectedMetaAccount()
            val voterAccountId = metaAccount.accountIdIn(governanceSelectedOption.assetWithChain.chain)

            locksOverviewFlowSuspend(voterAccountId, governanceSelectedOption)
        }
    }

    override fun unlockAffectsFlow(scope: CoroutineScope, assetFlow: Flow<Asset>): Flow<GovernanceUnlockAffects> {
        return flowOfAll {
            val governanceSelectedOption = selectedAssetState.selectedOption()
            val chain = governanceSelectedOption.assetWithChain.chain
            val chainAsset = governanceSelectedOption.assetWithChain.asset

            val governanceSource = governanceSourceRegistry.sourceFor(governanceSelectedOption)

            combine(
                assetFlow,
                balanceLocksRepository.observeBalanceLocks(chain, chainAsset),
                locksOverviewFlow(scope)
            ) { assetFlow, balanceLocks, locksOverview ->
                governanceSource.constructGovernanceUnlockAffects(assetFlow, balanceLocks, locksOverview)
            }
        }.distinctUntilChanged()
    }

    private suspend fun locksOverviewFlowSuspend(
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption
    ): Flow<GovernanceLocksOverview> {
        val chain = selectedGovernanceOption.assetWithChain.chain
        val asset = selectedGovernanceOption.assetWithChain.asset

        val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)
        val tracksById = governanceSource.referenda.getTracksById(chain.id)
        val undecidingTimeout = governanceSource.referenda.undecidingTimeout(chain.id)
        val voteLockingPeriod = governanceSource.convictionVoting.voteLockingPeriod(chain.id)

        val trackLocksFlow = governanceSource.convictionVoting.trackLocksFlowOrEmpty(voterAccountId, asset.fullId)

        val intermediateFlow = chainStateRepository.currentBlockNumberFlow(chain.id).map { currentBlockNumber ->
            val onChainReferenda = governanceSource.referenda.getAllOnChainReferenda(chain.id).associateBy(OnChainReferendum::id)
            val voting = voterAccountId?.let { governanceSource.convictionVoting.votingFor(voterAccountId, chain.id) }.orEmpty()
            val blockTime = chainStateRepository.predictedBlockTime(chain.id)
            val durationEstimator = BlockDurationEstimator(currentBlockNumber, blockTime)

            IntermediateData(voting, currentBlockNumber, onChainReferenda, durationEstimator)
        }

        return combine(intermediateFlow, trackLocksFlow) { intermediateData, trackLocks ->
            val claimScheduleCalculator = with(intermediateData) {
                RealClaimScheduleCalculator(voting, currentBlockNumber, onChainReferenda, tracksById, undecidingTimeout, voteLockingPeriod, trackLocks)
            }

            val claimSchedule = claimScheduleCalculator.estimateClaimSchedule()
            val locks = claimSchedule.toOverviewLocks(intermediateData.durationEstimator)

            GovernanceLocksOverview(
                totalLocked = claimScheduleCalculator.totalGovernanceLock(),
                locks = locks,
                claimSchedule = claimSchedule
            )
        }
    }

    private fun ClaimSchedule.toOverviewLocks(durationEstimator: BlockDurationEstimator): List<GovernanceLocksOverview.Lock> {
        return chunks.map { chunk ->
            when (chunk) {
                is UnlockChunk.Claimable -> GovernanceLocksOverview.Lock.Claimable(chunk.amount, chunk.actions)

                is UnlockChunk.Pending -> GovernanceLocksOverview.Lock.Pending(
                    amount = chunk.amount,
                    claimTime = when (val claimTime = chunk.claimableAt) {
                        is ClaimTime.At -> GovernanceLocksOverview.ClaimTime.At(durationEstimator.timerUntil(claimTime.block))
                        ClaimTime.UntilAction -> GovernanceLocksOverview.ClaimTime.UntilAction
                    }
                )
            }
        }
    }

    private fun GovernanceSource.constructGovernanceUnlockAffects(
        asset: Asset,
        balanceLocks: List<BalanceLock>,
        locksOverview: GovernanceLocksOverview,
    ): GovernanceUnlockAffects {
        val claimable = locksOverview.claimSchedule.claimableChunk()

        return if (claimable != null) {
            val newGovernanceLock = locksOverview.totalLocked - claimable.amount

            val transferableCurrent = asset.transferableInPlanks
            val newTotalLocked = balanceLocks.maxLockReplacing(convictionVoting.voteLockId, replaceWith = newGovernanceLock)
            val newTransferable = asset.transferableReplacingFrozen(newTotalLocked)

            val governanceLockChange = claimable.amount
            val transferableChange = (newTransferable - transferableCurrent).abs()

            val remainsLocked = governanceLockChange - transferableChange

            val remainsLockedInfo = if (remainsLocked.isPositive()) {
                RemainsLockedInfo(
                    amount = remainsLocked,
                    lockedInIds = balanceLocks.otherLocksPreventingLockBeingLessThan(newGovernanceLock, thisLockId = convictionVoting.voteLockId)
                )
            } else {
                null
            }

            GovernanceUnlockAffects(
                transferableChange = Change(
                    previousValue = transferableCurrent,
                    newValue = newTransferable,
                ),
                governanceLockChange = Change(
                    previousValue = locksOverview.totalLocked,
                    newValue = newGovernanceLock,
                ),
                claimableChunk = claimable,
                remainsLockedInfo = remainsLockedInfo,
            )
        } else {
            constructEmptyUnlockAffects(asset, locksOverview.totalLocked)
        }
    }

    private fun List<BalanceLock>.otherLocksPreventingLockBeingLessThan(amount: Balance, thisLockId: String): List<String> {
        return filter { it.id != thisLockId }.mapNotNull { lock ->
            lock.id.takeIf { lock.amountInPlanks > amount }
        }
    }

    private fun constructEmptyUnlockAffects(
        asset: Asset,
        totalGovernanceLock: Balance,
    ): GovernanceUnlockAffects {
        return GovernanceUnlockAffects(
            transferableChange = Change.Same(asset.transferableInPlanks),
            governanceLockChange = Change.Same(totalGovernanceLock),
            claimableChunk = null,
            remainsLockedInfo = null,
        )
    }
}

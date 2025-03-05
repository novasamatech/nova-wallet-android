package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.lockDuration
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.DelegateAssistant
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.ReusableLock
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.addIfPositive
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLockId
import io.novafoundation.nova.feature_wallet_api.domain.model.maxLockReplacing
import io.novafoundation.nova.feature_wallet_api.domain.model.transferableReplacingFrozen
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.util.BlockDurationEstimator

class RealDelegateAssistant(
    private val voteLockingPeriod: BlockNumber,
    private val balanceLocks: List<BalanceLock>,
    private val blockDurationEstimator: BlockDurationEstimator,
    private val votingLockId: BalanceLockId,
) : DelegateAssistant {

    private val currentMaxGovernanceLocked = balanceLocks.findById(votingLockId)?.amountInPlanks.orZero()
    private val allMaxLocked = balanceLocks.maxOfOrNull { it.amountInPlanks }.orZero()
    private val otherMaxLocked = balanceLocks.maxLockReplacing(votingLockId, replaceWith = Balance.ZERO)

    override suspend fun estimateLocksAfterDelegating(amount: Balance, conviction: Conviction, asset: Asset): LocksChange {
        val unlockBlocks = conviction.lockDuration(voteLockingPeriod)
        val unlockDuration = blockDurationEstimator.durationOf(unlockBlocks)

        val newGovernanceLocked = currentMaxGovernanceLocked.max(amount)

        val currentTransferablePlanks = asset.transferableInPlanks
        val newLocked = otherMaxLocked.max(newGovernanceLocked)
        val newTransferablePlanks = asset.transferableReplacingFrozen(newLocked)

        return LocksChange(
            lockedAmountChange = Change(
                previousValue = currentMaxGovernanceLocked,
                newValue = newGovernanceLocked,
            ),
            lockedPeriodChange = Change.Same(unlockDuration),
            transferableChange = Change(
                previousValue = currentTransferablePlanks,
                newValue = newTransferablePlanks,
            )
        )
    }

    override suspend fun reusableLocks(): List<ReusableLock> {
        return buildList {
            addIfPositive(ReusableLock.Type.ALL, allMaxLocked)
        }
    }
}

package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceLocksOverview
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceLocksOverview.ClaimTime
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceLocksOverview.Lock
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceUnlockInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.canClaimTokens
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel.StatusContent
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GovernanceLocksOverviewViewModel(
    private val router: GovernanceRouter,
    private val interactor: GovernanceUnlockInteractor,
    private val tokenUseCase: TokenUseCase,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val lockOverviewFlow = interactor.locksOverviewFlow(scope = viewModelScope)
        .shareInBackground()

    private val tokenFlow = tokenUseCase.currentTokenFlow()
        .shareInBackground()

    val totalAmount = combine(lockOverviewFlow, tokenFlow) { locksOverview, token ->
        mapAmountToAmountModel(locksOverview.totalLocked, token)
    }.shareInBackground()

    val lockModels = combine(lockOverviewFlow, tokenFlow) { locksOverview, token ->
        locksOverview.locks.mapIndexed { index, lock -> mapUnlockChunkToUi(lock, index, token) }
    }
        .withLoading()
        .shareInBackground()

    val isUnlockAvailable = lockOverviewFlow.map(GovernanceLocksOverview::canClaimTokens)
        .withLoading()
        .share()

    fun backClicked() {
        router.back()
    }

    fun unlockClicked() {
        router.openConfirmGovernanceUnlock()
    }

    private fun mapUnlockChunkToUi(lock: Lock, index: Int, token: Token): GovernanceLockModel {
        return when {
            lock is Lock.Claimable -> GovernanceLockModel(
                index = index,
                amount = mapAmountToAmountModel(lock.amount, token).token,
                status = StatusContent.Text(resourceManager.getString(R.string.referendum_unlock_unlockable)),
                statusColorRes = R.color.text_positive,
                statusIconRes = null,
                statusIconColorRes = null
            )
            lock is Lock.Pending && lock.claimTime is ClaimTime.At -> GovernanceLockModel(
                index = index,
                amount = mapAmountToAmountModel(lock.amount, token).token,
                status = StatusContent.Timer(lock.claimTime.timer),
                statusColorRes = R.color.text_secondary,
                statusIconRes = R.drawable.ic_time_16,
                statusIconColorRes = R.color.icon_secondary
            )

            lock is Lock.Pending && lock.claimTime is ClaimTime.UntilAction -> GovernanceLockModel(
                index = index,
                amount = mapAmountToAmountModel(lock.amount, token).token,
                status = StatusContent.Text(resourceManager.getString(R.string.delegation_your_delegation)),
                statusColorRes = R.color.text_secondary,
                statusIconRes = null,
                statusIconColorRes = null
            )

            else -> error("Not possible")
        }
    }
}

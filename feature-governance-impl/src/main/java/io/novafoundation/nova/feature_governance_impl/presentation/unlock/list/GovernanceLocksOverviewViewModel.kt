package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list.GovernanceLocksOverview
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list.GovernanceLocksOverview.Lock
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list.GovernanceLocksOverviewInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list.canClaimTokens
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel.StatusContent
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GovernanceLocksOverviewViewModel(
    private val router: GovernanceRouter,
    private val interactor: GovernanceLocksOverviewInteractor,
    private val tokenUseCase: TokenUseCase,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val lockOverViewFlow = interactor.locksOverviewFlow(scope = viewModelScope)
        .shareInBackground()

    private val tokenFlow = tokenUseCase.currentTokenFlow()
        .shareInBackground()

    val totalAmount = combine(lockOverViewFlow, tokenFlow) { locksOverview, token ->
        mapAmountToAmountModel(locksOverview.totalLocked, token)
    }.shareInBackground()

    val lockModels = combine(lockOverViewFlow, tokenFlow) { locksOverview, token ->
        locksOverview.claimSchedule.mapIndexed { index, lock -> mapUnlockChunkToUi(lock, index, token) }
    }
        .withLoading()
        .shareInBackground()

    val isUnlockAvailable = lockOverViewFlow.map(GovernanceLocksOverview::canClaimTokens)
        .withLoading()
        .share()

    fun backClicked() {
        router.back()
    }

    fun unlockClicked() {
        router.openConfirmGovernanceUnlock()
    }

    private fun mapUnlockChunkToUi(lock: Lock, index: Int, token: Token): GovernanceLockModel {
        return when (lock) {
            is Lock.Claimable -> GovernanceLockModel(
                index = index,
                amount = mapAmountToAmountModel(lock.amount, token).token,
                status = StatusContent.Text(resourceManager.getString(R.string.referendum_unlock_unlockable)),
                statusColorRes = R.color.multicolor_green_100,
                statusIconRes = null,
                statusIconColorRes = null
            )
            is Lock.Pending -> GovernanceLockModel(
                index = index,
                amount = mapAmountToAmountModel(lock.amount, token).token,
                status = StatusContent.Timer(lock.timer),
                statusIconColorRes = R.color.white_48,
                statusIconRes = R.drawable.ic_time_16,
                statusColorRes = R.color.white_64
            )
        }
    }
}

package io.novafoundation.nova.feature_push_notifications.data.presentation.settings

import android.Manifest
import android.os.Build
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatBooleanToState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsRequester
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.domain.model.isAnyGovEnabled
import io.novafoundation.nova.feature_push_notifications.data.domain.model.isNotEmpty
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val MAX_WALLETS = 3

class PushSettingsViewModel(
    private val router: PushNotificationsRouter,
    private val pushNotificationsInteractor: PushNotificationsInteractor,
    private val resourceManager: ResourceManager,
    private val walletRequester: SelectMultipleWalletsRequester,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val permissionsAsker: PermissionsAsker.Presentation,
) : BaseViewModel() {

    val closeConfirmationAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    private val oldPushSettingsState = flowOf { pushNotificationsInteractor.getPushSettings() }

    val pushEnabledState = MutableStateFlow(pushNotificationsInteractor.isPushNotificationsEnabled())
    private val pushSettingsState = MutableStateFlow<PushSettings?>(null)

    val pushSettingsWasChangedState = combine(pushEnabledState, pushSettingsState, oldPushSettingsState) { pushEnabled, newState, oldState ->
        pushEnabled != pushNotificationsInteractor.isPushNotificationsEnabled() ||
            newState != oldState
    }

    val pushWalletsQuantity = pushSettingsState
        .mapNotNull { it?.subscribedMetaAccounts?.size?.format() }
        .distinctUntilChanged()

    val pushAnnouncements = pushSettingsState
        .mapNotNull { it?.announcementsEnabled }
        .distinctUntilChanged()

    val pushSentTokens = pushSettingsState
        .mapNotNull { it?.sentTokensEnabled }
        .distinctUntilChanged()

    val pushReceivedTokens = pushSettingsState
        .mapNotNull { it?.receivedTokensEnabled }
        .distinctUntilChanged()

    val pushGovernanceState = pushSettingsState
        .mapNotNull { it }
        .map { resourceManager.formatBooleanToState(it.isAnyGovEnabled()) }
        .distinctUntilChanged()

    val pushStakingRewardsState = pushSettingsState
        .mapNotNull { it }
        .map { resourceManager.formatBooleanToState(it.stakingReward.isNotEmpty()) }
        .distinctUntilChanged()

    private val _savingInProgress = MutableStateFlow(false)
    val savingInProgress: Flow<Boolean> = _savingInProgress

    init {
        launch {
            pushSettingsState.value = oldPushSettingsState.first()
        }

        subscribeOnSelectWallets()
    }

    fun backClicked() {
        launch {
            if (pushSettingsWasChangedState.first()) {
                closeConfirmationAction.awaitAction(
                    ConfirmationDialogInfo(
                        R.string.common_confirmation_title,
                        R.string.common_close_confirmation_message,
                        R.string.common_close,
                        R.string.common_cancel,
                    )
                )
            }

            router.back()
        }
    }

    fun saveClicked() {
        launch {
            _savingInProgress.value = true
            val pushSettings = pushSettingsState.value ?: return@launch
            pushNotificationsInteractor.updatePushSettings(pushEnabledState.value, pushSettings)
                .onSuccess { router.back() }

            _savingInProgress.value = false
        }
    }

    fun enableSwitcherClicked() {
        launch {
            if (pushEnabledState.value == false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val isPermissionsGranted = permissionsAsker.requirePermissionsOrExit(Manifest.permission.POST_NOTIFICATIONS)

                if (!isPermissionsGranted) {
                    return@launch
                }
            }

            pushEnabledState.toggle()
        }
    }

    fun walletsClicked() {
        walletRequester.openRequest(
            SelectMultipleWalletsRequester.Request(
                titleText = resourceManager.getString(R.string.push_wallets_title),
                currentlySelectedMetaIds = pushSettingsState.value?.subscribedMetaAccounts?.toSet().orEmpty(),
                max = MAX_WALLETS
            )
        )
    }

    fun announementsClicked() {
        pushSettingsState.updateValue { it?.copy(announcementsEnabled = !it.announcementsEnabled) }
    }

    fun sentTokensClicked() {
        pushSettingsState.updateValue { it?.copy(sentTokensEnabled = !it.sentTokensEnabled) }
    }

    fun receivedTokensClicked() {
        pushSettingsState.updateValue { it?.copy(receivedTokensEnabled = !it.receivedTokensEnabled) }
    }

    fun governanceClicked() {
        TODO()
    }

    fun stakingRewardsClicked() {
        TODO()
    }

    private fun subscribeOnSelectWallets() {
        walletRequester.responseFlow
            .onEach {
                pushSettingsState.value = pushSettingsState.value
                    ?.copy(subscribedMetaAccounts = it.selectedMetaIds)
            }
            .launchIn(this)
    }
}

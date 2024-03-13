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
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.fromTrackIds
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.toTrackIds
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.domain.model.isGovEnabled
import io.novafoundation.nova.feature_push_notifications.data.domain.model.isNotEmpty
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsPayload
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsRequester
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsResponder
import io.novafoundation.nova.feature_push_notifications.data.presentation.staking.PushStakingSettingsPayload
import io.novafoundation.nova.feature_push_notifications.data.presentation.staking.PushStakingSettingsRequester
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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

private const val MIN_WALLETS = 1
private const val MAX_WALLETS = 3

class PushSettingsViewModel(
    private val router: PushNotificationsRouter,
    private val pushNotificationsInteractor: PushNotificationsInteractor,
    private val resourceManager: ResourceManager,
    private val walletRequester: SelectMultipleWalletsRequester,
    private val pushGovernanceSettingsRequester: PushGovernanceSettingsRequester,
    private val pushStakingSettingsRequester: PushStakingSettingsRequester,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val permissionsAsker: PermissionsAsker.Presentation,
) : BaseViewModel() {

    val closeConfirmationAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    private val oldPushSettingsState = flowOf { pushNotificationsInteractor.getPushSettings() }
        .shareInBackground()

    val pushEnabledState = MutableStateFlow(pushNotificationsInteractor.isPushNotificationsEnabled())
    private val pushSettingsState = MutableStateFlow<PushSettings?>(null)

    val pushSettingsWasChangedState = combine(pushEnabledState, pushSettingsState, oldPushSettingsState) { pushEnabled, newState, oldState ->
        pushEnabled != pushNotificationsInteractor.isPushNotificationsEnabled() ||
            newState != oldState
    }

    val pushWalletsQuantity = pushSettingsState
        .mapNotNull { it?.subscribedMetaAccounts?.size?.format() }
        .distinctUntilChanged()

    val pushAnnouncements = pushSettingsState.mapNotNull { it?.announcementsEnabled }
        .distinctUntilChanged()

    val pushSentTokens = pushSettingsState.mapNotNull { it?.sentTokensEnabled }
        .distinctUntilChanged()

    val pushReceivedTokens = pushSettingsState.mapNotNull { it?.receivedTokensEnabled }
        .distinctUntilChanged()

    val pushGovernanceState = pushSettingsState.mapNotNull { it }
        .map { resourceManager.formatBooleanToState(it.isGovEnabled()) }
        .distinctUntilChanged()

    val pushStakingRewardsState = pushSettingsState.mapNotNull { it }
        .map { resourceManager.formatBooleanToState(it.stakingReward.isNotEmpty()) }
        .distinctUntilChanged()

    val showNoSelectedWalletsTip = pushSettingsState
        .mapNotNull { it?.subscribedMetaAccounts?.isEmpty() }
        .distinctUntilChanged()

    private val _savingInProgress = MutableStateFlow(false)
    val savingInProgress: Flow<Boolean> = _savingInProgress

    init {
        launch {
            pushSettingsState.value = oldPushSettingsState.first()
        }

        subscribeOnSelectWallets()
        subscribeOnGovernanceSettings()
        subscribeOnStakingSettings()
        disableNotificationsIfPushSettingsEmpty()
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
                .onFailure { showError(it) }

            _savingInProgress.value = false
        }
    }

    fun enableSwitcherClicked() {
        launch {
            if (!pushEnabledState.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val isPermissionsGranted = permissionsAsker.requirePermissionsOrExit(Manifest.permission.POST_NOTIFICATIONS)

                if (!isPermissionsGranted) {
                    return@launch
                }
            }

            if (!pushEnabledState.value) {
                makeDefaultPushSettingsIfNeeded()
            }

            pushEnabledState.toggle()
        }
    }

    fun walletsClicked() {
        walletRequester.openRequest(
            SelectMultipleWalletsRequester.Request(
                titleText = resourceManager.getString(R.string.push_wallets_title),
                currentlySelectedMetaIds = pushSettingsState.value?.subscribedMetaAccounts?.toSet().orEmpty(),
                min = MIN_WALLETS,
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
        val settings = pushSettingsState.value ?: return
        pushGovernanceSettingsRequester.openRequest(PushGovernanceSettingsRequester.Request(mapGovSettingsToPayload(settings)))
    }

    fun stakingRewardsClicked() {
        val stakingRewards = pushSettingsState.value?.stakingReward ?: return
        val settings = when (stakingRewards) {
            is PushSettings.ChainFeature.All -> PushStakingSettingsPayload.AllChains
            is PushSettings.ChainFeature.Concrete -> PushStakingSettingsPayload.SpecifiedChains(stakingRewards.chainIds.toSet())
        }
        val request = PushStakingSettingsRequester.Request(settings)
        pushStakingSettingsRequester.openRequest(request)
    }

    private fun subscribeOnSelectWallets() {
        walletRequester.responseFlow
            .onEach {
                pushSettingsState.value = pushSettingsState.value
                    ?.copy(subscribedMetaAccounts = it.selectedMetaIds)
            }
            .launchIn(this)
    }

    private fun subscribeOnGovernanceSettings() {
        pushGovernanceSettingsRequester.responseFlow
            .onEach { response ->
                pushSettingsState.updateValue { settings ->
                    settings?.copy(governance = mapGovSettingsReponseToModel(response))
                }
            }
            .launchIn(this)
    }

    private fun subscribeOnStakingSettings() {
        pushStakingSettingsRequester.responseFlow
            .onEach { response ->
                val stakingSettings = when (response.settings) {
                    is PushStakingSettingsPayload.AllChains -> PushSettings.ChainFeature.All
                    is PushStakingSettingsPayload.SpecifiedChains -> PushSettings.ChainFeature.Concrete(response.settings.enabledChainIds.toList())
                }

                pushSettingsState.updateValue { settings ->
                    settings?.copy(stakingReward = stakingSettings)
                }
            }
            .launchIn(this)
    }

    private fun mapGovSettingsToPayload(pushSettings: PushSettings): List<PushGovernanceSettingsPayload> {
        return pushSettings.governance.map { (chainId, govState) ->
            PushGovernanceSettingsPayload(
                chainId = chainId,
                governance = Chain.Governance.V2,
                newReferenda = govState.newReferendaEnabled,
                referendaUpdates = govState.referendumUpdateEnabled,
                delegateVotes = govState.govMyDelegateVotedEnabled,
                tracksIds = govState.tracks.fromTrackIds()
            )
        }
    }

    private fun mapGovSettingsReponseToModel(response: PushGovernanceSettingsResponder.Response): Map<ChainId, PushSettings.GovernanceState> {
        return response.enabledGovernanceSettings
            .associateBy { it.chainId }
            .mapValues { (_, govState) ->
                PushSettings.GovernanceState(
                    newReferendaEnabled = govState.newReferenda,
                    referendumUpdateEnabled = govState.referendaUpdates,
                    govMyDelegateVotedEnabled = govState.delegateVotes,
                    tracks = govState.tracksIds.toTrackIds()
                )
            }
    }

    private fun disableNotificationsIfPushSettingsEmpty() {
        pushSettingsState
            .onEach { pushSettings ->
                if (pushSettings?.settingsIsEmpty() == true) {
                    pushEnabledState.value = false
                }
            }
            .launchIn(this)
    }

    private suspend fun makeDefaultPushSettingsIfNeeded() {
        if (pushSettingsState.value?.settingsIsEmpty() == true) {
            pushSettingsState.value = pushNotificationsInteractor.getPushSettings()
        }
    }
}

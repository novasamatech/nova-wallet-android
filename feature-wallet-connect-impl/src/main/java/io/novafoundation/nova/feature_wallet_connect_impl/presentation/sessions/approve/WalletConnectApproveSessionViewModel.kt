package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.navigation.requireLastInput
import io.novafoundation.nova.common.navigation.respond
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatListPreview
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainListOverview
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.selectedMetaAccount
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.SessionChains
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionProposal
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.allKnownChains
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.allUnknownChains
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.dAppTitle
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.hasAny
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.hasUnknown
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.model.SessionAlerts
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.model.hasBlockingAlerts
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.view.WCNetworkListModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val MISSING_ACCOUNTS_PREVIEW_SIZE = 3

class WalletConnectApproveSessionViewModel(
    private val router: WalletConnectRouter,
    private val interactor: WalletConnectSessionInteractor,
    private val responder: ApproveSessionResponder,
    private val resourceManager: ResourceManager,
    private val selectWalletMixinFactory: SelectWalletMixin.Factory
) : BaseViewModel() {

    private val proposal = responder.requireLastInput()

    val selectWalletMixin = selectWalletMixinFactory.create(
        coroutineScope = this,
        selectionParams = ::walletSelectionParams
    )

    private val processState = MutableStateFlow(ProgressState.IDLE)

    private val sessionProposalFlow = flowOf {
        interactor.resolveSessionProposal(proposal)
    }.shareInBackground()

    val sessionMetadata = sessionProposalFlow.map { it.dappMetadata }

    val title = sessionMetadata.map { sessionDAppMetadata ->
        val dAppTitle = sessionDAppMetadata.dAppTitle

        resourceManager.getString(R.string.dapp_confirm_authorize_title_format, dAppTitle)
    }.shareInBackground()

    val chainsOverviewFlow = sessionProposalFlow.map { sessionProposal ->
        createSessionNetworksModel(sessionProposal.resolvedChains)
    }.shareInBackground()

    val sessionAlerts = combine(selectWalletMixin.selectedMetaAccountFlow, sessionProposalFlow) { metaAccount, sessionProposal ->
        constructSessionAlerts(metaAccount, sessionProposal)
    }.shareInBackground()

    val networksListFlow = sessionProposalFlow.map { constructNetworksList(it.resolvedChains) }
        .shareInBackground()

    val allowButtonState = allowButtonState().shareInBackground()
    val rejectButtonState = rejectButtonState().shareInBackground()

    private val _showNetworksBottomSheet = MutableLiveData<Event<List<WCNetworkListModel>>>()
    val showNetworksBottomSheet: LiveData<Event<List<WCNetworkListModel>>> = _showNetworksBottomSheet

    fun exit() {
        rejectClicked()
    }

    fun rejectClicked() = launch {
        if (isInProgress()) return@launch
        processState.value = ProgressState.REJECTING

        val proposal = responder.requireLastInput()

        interactor.rejectSession(proposal)
        responder.respond()
        router.back()
    }

    fun approveClicked() = launch {
        if (isInProgress()) return@launch
        processState.value = ProgressState.CONFIRMING

        val proposal = responder.requireLastInput()
        val metaAccount = selectWalletMixin.selectedMetaAccount()

        interactor.approveSession(proposal, metaAccount)
            .onFailure {
                Log.d("WalletConnect", "Session approve failed", it)
            }

        responder.respond()
        router.back()
    }

    fun networksClicked() = launch {
        _showNetworksBottomSheet.value = networksListFlow.first().event()
    }

    private suspend fun walletSelectionParams(): SelectWalletMixin.SelectionParams {
        val pairingAccount = interactor.getPairingAccount(proposal.pairingTopic)

        return if (pairingAccount != null) {
            SelectWalletMixin.SelectionParams(
                selectionAllowed = false,
                initialSelection = SelectWalletMixin.InitialSelection.SpecificWallet(pairingAccount.metaId)
            )
        } else {
            SelectWalletMixin.SelectionParams(
                selectionAllowed = true,
                initialSelection = SelectWalletMixin.InitialSelection.ActiveWallet
            )
        }
    }

    private fun constructSessionAlerts(metaAccount: MetaAccount, sessionProposal: WalletConnectSessionProposal): SessionAlerts {
        val chains = sessionProposal.resolvedChains

        val unsupportedChainsAlert = if (chains.required.hasUnknown()) {
            val content = resourceManager.getString(R.string.wallet_connect_session_approve_unsupported_chains_alert, sessionProposal.dappMetadata.dAppTitle)

            SessionAlerts.UnsupportedChains(content)
        } else {
            null
        }

        val chainsWithMissingAccounts = metaAccount.findMissingAccountsFor(chains.required.knownChains)

        val missingAccountsAlert = if (chainsWithMissingAccounts.isNotEmpty()) {
            val missingChainNames = chainsWithMissingAccounts.map { it.name }
            val missingChains = resourceManager.formatListPreview(missingChainNames, maxPreviewItems = MISSING_ACCOUNTS_PREVIEW_SIZE)
            val content = resourceManager.getQuantityString(
                R.plurals.wallet_connect_session_approve_missing_accounts_alert,
                chainsWithMissingAccounts.size,
                missingChains
            )

            SessionAlerts.MissingAccounts(content)
        } else {
            null
        }

        return SessionAlerts(
            missingAccounts = missingAccountsAlert,
            unsupportedChains = unsupportedChainsAlert
        )
    }

    private fun rejectButtonState(): Flow<DescriptiveButtonState> {
        return processState.map { progressState ->
            when (progressState) {
                ProgressState.IDLE -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_reject))

                ProgressState.REJECTING -> DescriptiveButtonState.Loading

                else -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_reject))
            }
        }
    }

    private fun allowButtonState(): Flow<DescriptiveButtonState> {
        return combine(processState, sessionAlerts) { progressState, sessionAlerts ->
            when {
                sessionAlerts.hasBlockingAlerts() -> DescriptiveButtonState.Gone

                progressState == ProgressState.IDLE -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_allow))

                progressState == ProgressState.CONFIRMING -> DescriptiveButtonState.Loading

                else -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_allow))
            }
        }
    }

    private fun isInProgress(): Boolean {
        return processState.value != ProgressState.IDLE
    }

    @Suppress("KotlinConstantConditions")
    private fun createSessionNetworksModel(sessionChains: SessionChains): ChainListOverview {
        val allKnownChains = sessionChains.allKnownChains()
        val allUnknownChains = sessionChains.allUnknownChains()

        val allChainsCount = allKnownChains.size + allUnknownChains.size

        val value = when {
            // no chains
            allKnownChains.isEmpty() && allUnknownChains.isEmpty() -> resourceManager.getString(R.string.common_none)

            // only unknown chains
            allKnownChains.isEmpty() && allUnknownChains.isNotEmpty() -> {
                resourceManager.getQuantityString(R.plurals.common_unknown_chains, allUnknownChains.size, allUnknownChains.size)
            }

            // single known chain
            allKnownChains.size == 1 && allUnknownChains.isEmpty() -> {
                allKnownChains.single().name
            }

            // multiple known and unknown chains
            else -> {
                val previewItem = allKnownChains.first().name
                val othersCount = allChainsCount - 1

                resourceManager.getString(R.string.common_element_and_more_format, previewItem, othersCount)
            }
        }

        val multipleChainsRequested = allChainsCount > 1
        val hasUnsupportedWarningsToShow = allUnknownChains.isNotEmpty()

        val firstKnownIcon = allKnownChains.firstOrNull()?.iconOrFallback()

        return ChainListOverview(
            icon = firstKnownIcon?.takeUnless { multipleChainsRequested },
            value = value,
            label = resourceManager.getQuantityString(R.plurals.common_networks_plural, allChainsCount),
            hasMoreElements = multipleChainsRequested || hasUnsupportedWarningsToShow
        )
    }

    private fun MetaAccount.findMissingAccountsFor(chains: Collection<Chain>): List<Chain> {
        return chains.filterNot(::hasAccountIn)
    }

    private fun constructNetworksList(sessionChains: SessionChains): List<WCNetworkListModel> {
        return buildList {
            addCategory(sessionChains.required, R.string.common_required)
            addCategory(sessionChains.optional, R.string.common_optional)
        }
    }

    private fun MutableList<WCNetworkListModel>.addCategory(resolvedChains: SessionChains.ResolvedChains, @StringRes categoryNameRes: Int) {
        if (resolvedChains.hasAny()) {
            val element = WCNetworkListModel.Label(name = resourceManager.getString(categoryNameRes), needsAdditionalSeparator = true)
            add(element)
        }

        val knownChainsUi = resolvedChains.knownChains.map { WCNetworkListModel.Chain(mapChainToUi(it)) }
        addAll(knownChainsUi)

        if (resolvedChains.hasUnknown()) {
            val unknownCount = resolvedChains.unknownChains.size
            val unknownLabel = resourceManager.getQuantityString(R.plurals.wallet_connect_unsupported_networks_hidden, unknownCount, unknownCount)
            val element = WCNetworkListModel.Label(name = unknownLabel, needsAdditionalSeparator = false)
            add(element)
        }
    }
}

private enum class ProgressState {
    IDLE, CONFIRMING, REJECTING
}

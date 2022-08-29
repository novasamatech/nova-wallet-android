package io.novafoundation.nova.feature_account_impl.presentation.account.details

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.filterToSet
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountDetailsInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.presentation.account.details.model.AccountTypeAlert
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.AccountInChainUi
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private const val UPDATE_NAME_INTERVAL_SECONDS = 1L

class AccountDetailsViewModel(
    private val interactor: AccountDetailsInteractor,
    private val accountRouter: AccountRouter,
    private val iconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val metaId: Long,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
    private val addAccountLauncherMixin: AddAccountLauncherMixin.Presentation,
) : BaseViewModel(),
    ExternalActions by externalActions,
    ImportTypeChooserMixin by importTypeChooserMixin,
    AddAccountLauncherMixin by addAccountLauncherMixin {

    val accountNameFlow: MutableStateFlow<String> = MutableStateFlow("")

    private val metaAccount = async(Dispatchers.Default) { interactor.getMetaAccount(metaId) }

    val availableAccountActions = flowOf {
        availableAccountActions(metaAccount().type)
    }.shareInBackground()

    val typeAlert = flowOf {
        accountTypeAlertFor(metaAccount().type)
    }.shareInBackground()

    val chainAccountProjections = flowOf { interactor.getChainProjections(metaAccount()) }
        .map { groupedList ->
            groupedList.toListWithHeaders(
                keyMapper = { mapFromToTextHeader(it) },
                valueMapper = { mapChainAccountProjectionToUi(metaAccount(), it) }
            )
        }
        .inBackground()
        .share()

    init {
        launch {
            accountNameFlow.emit(metaAccount().name)
        }

        syncNameChangesWithDb()
    }

    fun backClicked() {
        accountRouter.back()
    }

    @OptIn(ExperimentalTime::class)
    private fun syncNameChangesWithDb() {
        accountNameFlow
            .filter { it.isNotEmpty() }
            .debounce(UPDATE_NAME_INTERVAL_SECONDS.seconds)
            .onEach { interactor.updateName(metaId, it) }
            .launchIn(viewModelScope)
    }

    private suspend fun mapFromToTextHeader(from: AccountInChain.From): TextHeader? {
        val availableActions = availableAccountActions.first()

        // it is not possible to add chain account for this type of wallet
        // so do not show groups since there will only be one group (shared secrets)
        if (AccountAction.CHANGE !in availableActions) return null

        val resId = when (from) {
            AccountInChain.From.META_ACCOUNT -> R.string.account_shared_secret
            AccountInChain.From.CHAIN_ACCOUNT -> R.string.account_custom_secret
        }

        return TextHeader(resourceManager.getString(resId))
    }

    private suspend fun mapChainAccountProjectionToUi(metaAccount: LightMetaAccount, accountInChain: AccountInChain) = with(accountInChain) {
        val addressOrHint = when {
            projection != null -> projection.address
            metaAccount.type == Type.PARITY_SIGNER -> resourceManager.getString(R.string.account_details_parity_signer_not_supported)
            else -> resourceManager.getString(R.string.account_no_chain_projection)
        }

        val accountIcon = projection?.let {
            iconGenerator.createAddressIcon(it.accountId, AddressIconGenerator.SIZE_SMALL, backgroundColorRes = R.color.account_icon_dark)
        } ?: resourceManager.getDrawable(R.drawable.ic_warning_filled)

        val availableActionsForChain = availableActionsFor(accountInChain)
        val canViewAddresses = accountInChain.projection != null
        val canDoAnyActions = availableActionsForChain.isNotEmpty() || canViewAddresses

        AccountInChainUi(
            chainUi = mapChainToUi(chain),
            addressOrHint = addressOrHint,
            address = projection?.address,
            accountIcon = accountIcon,
            actionsAvailable = canDoAnyActions
        )
    }

    fun chainAccountClicked(item: AccountInChainUi) = launch {
        if (!item.actionsAvailable) return@launch

        val chain = chainRegistry.getChain(item.chainUi.id)

        val type = ExternalActions.Type.Address(item.address)

        externalActions.showExternalActions(type, chain)
    }

    fun exportClicked(inChain: Chain) = launch {
        viewModelScope.launch {
            val sources = interactor.availableExportTypes(metaAccount(), inChain)

            val payload = ImportTypeChooserMixin.Payload(
                allowedTypes = sources,
                onChosen = { exportTypeChosen(it, inChain) }
            )
            importTypeChooserMixin.showChooser(payload)
        }
    }

    fun changeChainAccountClicked(inChain: Chain) {
        launch {
            addAccountLauncherMixin.initiateLaunch(inChain, metaAccount())
        }
    }

    private fun availableAccountActions(accountType: Type): Set<AccountAction> {
        return when (accountType) {
            Type.SECRETS -> setOf(AccountAction.EXPORT, AccountAction.CHANGE)
            Type.WATCH_ONLY -> setOf(AccountAction.CHANGE)
            Type.PARITY_SIGNER -> emptySet()
            Type.LEDGER -> setOf(AccountAction.CHANGE)
        }
    }

    private fun accountTypeAlertFor(accountType: Type): AccountTypeAlert? {
        return when (accountType) {
            Type.WATCH_ONLY -> AccountTypeAlert(
                style = AlertView.Style(
                    backgroundColorRes = R.color.white_12,
                    iconRes = R.drawable.ic_watch
                ),
                text = resourceManager.getString(R.string.account_details_watch_only_alert)
            )
            Type.PARITY_SIGNER -> AccountTypeAlert(
                style = AlertView.Style(
                    backgroundColorRes = R.color.white_12,
                    iconRes = R.drawable.ic_parity_signer
                ),
                text = resourceManager.getString(R.string.account_details_parity_signer_alert)
            )
            Type.SECRETS -> null
            Type.LEDGER -> AccountTypeAlert(
                style = AlertView.Style(
                    backgroundColorRes = R.color.white_12,
                    iconRes = R.drawable.ic_ledger
                ),
                text = resourceManager.getString(R.string.ledger_wallet_details_description)
            )
        }
    }

    private fun exportTypeChosen(type: SecretType, chain: Chain) {
        val exportPayload = ExportPayload(metaId, chain.id)

        val navigationAction = when (type) {
            SecretType.MNEMONIC -> accountRouter.exportMnemonicAction(exportPayload)
            SecretType.SEED -> accountRouter.exportSeedAction(exportPayload)
            SecretType.JSON -> accountRouter.exportJsonPasswordAction(exportPayload)
        }

        accountRouter.withPinCodeCheckRequired(navigationAction)
    }

    private suspend fun availableActionsFor(accountInChain: AccountInChain): Set<AccountAction> {
        return availableAccountActions.first().filterToSet { action ->
            when (action) {
                AccountAction.CHANGE -> true
                AccountAction.EXPORT -> accountInChain.projection != null
            }
        }
    }
}

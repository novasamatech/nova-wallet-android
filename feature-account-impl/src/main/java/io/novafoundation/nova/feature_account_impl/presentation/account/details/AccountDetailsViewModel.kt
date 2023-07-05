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
import io.novafoundation.nova.feature_account_api.domain.model.asPolkadotVaultVariantOrThrow
import io.novafoundation.nova.feature_account_api.domain.model.isPolkadotVaultLike
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.formatWithPolkadotVaultLabel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountDetailsInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
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
import kotlin.time.Duration.Companion.seconds

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
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
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
                keyMapper = { type, _ -> mapFromToTextHeader(type) },
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

    private fun syncNameChangesWithDb() {
        accountNameFlow
            .filter { it.isNotEmpty() }
            .debounce(UPDATE_NAME_INTERVAL_SECONDS.seconds)
            .onEach { interactor.updateName(metaId, it) }
            .launchIn(viewModelScope)
    }

    private suspend fun mapFromToTextHeader(from: AccountInChain.From): TextHeader? {
        return when (metaAccount().type) {
            Type.LEDGER, Type.PARITY_SIGNER, Type.POLKADOT_VAULT -> null
            Type.SECRETS, Type.WATCH_ONLY -> {
                val resId = when (from) {
                    AccountInChain.From.META_ACCOUNT -> R.string.account_shared_secret
                    AccountInChain.From.CHAIN_ACCOUNT -> R.string.account_custom_secret
                }

                return TextHeader(resourceManager.getString(resId))
            }
        }
    }

    private suspend fun mapChainAccountProjectionToUi(metaAccount: LightMetaAccount, accountInChain: AccountInChain) = with(accountInChain) {
        val addressOrHint = when {
            projection != null -> projection.address
            metaAccount.type.isPolkadotVaultLike() -> {
                val polkadotVaultVariant = metaAccount.type.asPolkadotVaultVariantOrThrow()
                resourceManager.formatWithPolkadotVaultLabel(R.string.account_details_parity_signer_not_supported, polkadotVaultVariant)
            }
            else -> resourceManager.getString(R.string.account_no_chain_projection)
        }

        val accountIcon = projection?.let {
            iconGenerator.createAddressIcon(it.accountId, AddressIconGenerator.SIZE_SMALL, backgroundColorRes = AddressIconGenerator.BACKGROUND_TRANSPARENT)
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
            Type.PARITY_SIGNER, Type.POLKADOT_VAULT -> emptySet()
            Type.LEDGER -> setOf(AccountAction.CHANGE)
        }
    }

    private fun accountTypeAlertFor(accountType: Type): AccountTypeAlert? {
        return when (accountType) {
            Type.WATCH_ONLY -> AccountTypeAlert(
                style = AlertView.Style(
                    backgroundColorRes = R.color.block_background,
                    iconRes = R.drawable.ic_watch_only_filled
                ),
                text = resourceManager.getString(R.string.account_details_watch_only_alert)
            )
            Type.PARITY_SIGNER, Type.POLKADOT_VAULT -> {
                val polkadotVaultVariant = accountType.asPolkadotVaultVariantOrThrow()
                val variantConfig = polkadotVaultVariantConfigProvider.variantConfigFor(polkadotVaultVariant)

                AccountTypeAlert(
                    style = AlertView.Style(
                        backgroundColorRes = R.color.block_background,
                        iconRes = variantConfig.common.iconRes
                    ),
                    text = resourceManager.formatWithPolkadotVaultLabel(R.string.account_details_parity_signer_alert, polkadotVaultVariant)
                )
            }
            Type.SECRETS -> null
            Type.LEDGER -> AccountTypeAlert(
                style = AlertView.Style(
                    backgroundColorRes = R.color.block_background,
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

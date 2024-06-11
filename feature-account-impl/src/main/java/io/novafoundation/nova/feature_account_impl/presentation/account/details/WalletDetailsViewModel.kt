package io.novafoundation.nova.feature_account_impl.presentation.account.details

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.WalletDetailsMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.WalletDetailsMixinHost
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private const val UPDATE_NAME_INTERVAL_SECONDS = 1L

class WalletDetailsViewModel(
    private val interactor: WalletDetailsInteractor,
    private val accountRouter: AccountRouter,
    private val metaId: Long,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
    private val addAccountLauncherMixin: AddAccountLauncherMixin.Presentation,
    private val walletDetailsMixinFactory: WalletDetailsMixinFactory
) : BaseViewModel(),
    ExternalActions by externalActions,
    ImportTypeChooserMixin by importTypeChooserMixin,
    AddAccountLauncherMixin by addAccountLauncherMixin {

    private val detailsHost = WalletDetailsMixinHost(
        browserableDelegate = externalActions
    )

    private val walletDetailsMixin = async { walletDetailsMixinFactory.create(metaId, detailsHost) }

    val accountNameFlow: MutableStateFlow<String> = MutableStateFlow("")

    val availableAccountActions = flowOfAll { walletDetailsMixin().availableAccountActions }
        .shareInBackground()

    val typeAlert = flowOfAll { walletDetailsMixin().typeAlert }
        .shareInBackground()

    val chainAccountProjections = flowOfAll { walletDetailsMixin().chainAccountProjections }
        .shareInBackground()

    init {
        launch {
            accountNameFlow.emit(walletDetailsMixin().metaAccount.name)
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

    fun chainAccountClicked(item: AccountInChainUi) = launch {
        if (!item.actionsAvailable) return@launch

        val chain = chainRegistry.getChain(item.chainUi.id)

        val type = ExternalActions.Type.Address(item.address)

        externalActions.showExternalActions(type, chain)
    }

    fun exportClicked(inChain: Chain) = launch {
        viewModelScope.launch {
            val sources = interactor.availableExportTypes(walletDetailsMixin().metaAccount, inChain)

            val payload = ImportTypeChooserMixin.Payload(
                allowedTypes = sources,
                onChosen = { exportTypeChosen(it, inChain) }
            )
            importTypeChooserMixin.showChooser(payload)
        }
    }

    fun changeChainAccountClicked(inChain: Chain) {
        launch {
            addAccountLauncherMixin.initiateLaunch(inChain, walletDetailsMixin().metaAccount)
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
}

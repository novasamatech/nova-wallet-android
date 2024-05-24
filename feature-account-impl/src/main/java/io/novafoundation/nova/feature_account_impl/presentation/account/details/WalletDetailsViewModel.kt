package io.novafoundation.nova.feature_account_impl.presentation.account.details

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.WalletDetailsMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.AccountInChainUi
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherPresentationFactory
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class WalletDetailsViewModel(
    private val rootScope: RootScope,
    private val interactor: WalletDetailsInteractor,
    private val accountRouter: AccountRouter,
    private val metaId: Long,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
    private val addAccountLauncherPresentationFactory: AddAccountLauncherPresentationFactory,
    private val walletDetailsMixinFactory: WalletDetailsMixinFactory
) : BaseViewModel(),
    ExternalActions by externalActions,
    ImportTypeChooserMixin by importTypeChooserMixin {

    val addAccountLauncherMixin = addAccountLauncherPresentationFactory.create(viewModelScope)

    val walletDetailsMixin = async { walletDetailsMixinFactory.create(metaId) }

    private val startAccountName = async { walletDetailsMixin().metaAccount.name }

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
    }

    fun backClicked() {
        accountRouter.back()
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

    override fun onCleared() {
        // Launch it in root scope to avoid coroutine cancellation
        rootScope.launch {
            val newAccountName = accountNameFlow.value
            if (startAccountName() != newAccountName) {
                interactor.updateName(metaId, newAccountName)
            }
        }
    }

    private fun exportTypeChosen(type: SecretType, chain: Chain) {
        val exportPayload = ExportPayload.ChainAccount(metaId, chain.id)

        val navigationAction = when (type) {
            SecretType.MNEMONIC -> accountRouter.getExportMnemonicDelayedNavigation(exportPayload)
            SecretType.SEED -> accountRouter.getExportSeedDelayedNavigation(exportPayload)
            SecretType.JSON -> accountRouter.getExportJsonDelayedNavigation(exportPayload)
        }

        accountRouter.withPinCodeCheckRequired(navigationAction)
    }
}

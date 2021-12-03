package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin.Presentation
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin.Presentation.Mode
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class AddAccountLauncherProvider(
    private val importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
    private val resourceManager: ResourceManager,
    private val router: AccountRouter
) : Presentation {

    private fun addAccountSelected(chainAccountPayload: AddAccountPayload.ChainAccount) {
        router.openMnemonicScreen(accountName = null, chainAccountPayload)
    }

    private fun importAccountSelected(chainAccountPayload: AddAccountPayload.ChainAccount) {
        val payload = ImportTypeChooserMixin.Payload(
            onChosen = { importTypeSelected(chainAccountPayload, it) }
        )

        importTypeChooserMixin.showChooser(payload)
    }

    private fun importTypeSelected(chainAccountPayload: AddAccountPayload.ChainAccount, importType: ImportType) {
        router.openImportAccountScreen(ImportAccountPayload(importType, chainAccountPayload))
    }

    override fun initiateLaunch(chain: Chain, metaAccountId: Long, mode: Mode) {
        val chainAccountPayload = AddAccountPayload.ChainAccount(chain.id, metaAccountId)

        val titleTemplate = when (mode) {
            Mode.CHANGE -> R.string.accounts_change_chain_account
            Mode.ADD -> R.string.accounts_add_chain_account
        }
        val title = resourceManager.getString(titleTemplate, chain.name)

        showAddAccountTypeChooser.value = AddAccountLauncherMixin.AddAccountTypePayload(
            title = title,
            onCreate = { addAccountSelected(chainAccountPayload) },
            onImport = { importAccountSelected(chainAccountPayload) }
        ).event()
    }

    override val showAddAccountTypeChooser = MutableLiveData<Event<AddAccountLauncherMixin.AddAccountTypePayload>>()

    override val showImportTypeChooser: LiveData<Event<ImportTypeChooserMixin.Payload>> = importTypeChooserMixin.showChooserEvent
}

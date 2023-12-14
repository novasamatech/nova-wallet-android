package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_api.presenatation.account.add.asImportType
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin.Presentation
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

    private fun importTypeSelected(chainAccountPayload: AddAccountPayload.ChainAccount, secretType: SecretType) {
        router.openImportAccountScreen(ImportAccountPayload(secretType.asImportType(), chainAccountPayload))
    }

    override fun initiateLaunch(chain: Chain, metaAccount: MetaAccount) {
        when (metaAccount.type) {
            LightMetaAccount.Type.SECRETS -> launchAddFromSecrets(chain, metaAccount)
            LightMetaAccount.Type.WATCH_ONLY -> launchAddWatchOnly(chain, metaAccount)
            LightMetaAccount.Type.LEDGER -> launchAddLedger(chain, metaAccount)
            // adding chain accounts is not supported for Polkadot Vault like wallets and for Proxied wallets
            LightMetaAccount.Type.PARITY_SIGNER, LightMetaAccount.Type.POLKADOT_VAULT, LightMetaAccount.Type.PROXIED -> {}
        }
    }

    private fun launchAddLedger(chain: Chain, metaAccount: MetaAccount) {
        val chainAccountPayload = AddAccountPayload.ChainAccount(chain.id, metaAccount.id)

        router.openAddLedgerChainAccountFlow(chainAccountPayload)
    }

    private fun launchAddWatchOnly(chain: Chain, metaAccount: MetaAccount) {
        val chainAccountPayload = AddAccountPayload.ChainAccount(chain.id, metaAccount.id)

        router.openChangeWatchAccount(chainAccountPayload)
    }

    private fun launchAddFromSecrets(chain: Chain, metaAccount: MetaAccount) {
        val chainAccountPayload = AddAccountPayload.ChainAccount(chain.id, metaAccount.id)

        val titleTemplate = if (metaAccount.hasAccountIn(chain)) {
            R.string.accounts_change_chain_account
        } else {
            R.string.accounts_add_chain_account
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

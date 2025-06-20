package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import io.novafoundation.nova.feature_account_api.presenatation.account.add.asImportType
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin.Presentation
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RealAddAccountLauncherPresentationFactory(
    private val cloudBackupService: CloudBackupService,
    private val importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
    private val resourceManager: ResourceManager,
    private val router: AccountRouter,
    private val addAccountInteractor: AddAccountInteractor,
    private val cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory
) : AddAccountLauncherPresentationFactory {

    override fun create(scope: CoroutineScope): Presentation {
        return AddAccountLauncherProvider(
            cloudBackupService,
            importTypeChooserMixin,
            resourceManager,
            router,
            addAccountInteractor,
            scope,
            cloudBackupChangingWarningMixinFactory
        )
    }
}

class AddAccountLauncherProvider(
    private val cloudBackupService: CloudBackupService,
    private val importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
    private val resourceManager: ResourceManager,
    private val router: AccountRouter,
    private val addAccountInteractor: AddAccountInteractor,
    private val scope: CoroutineScope,
    cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory
) : Presentation {

    override val cloudBackupChangingWarningMixin = cloudBackupChangingWarningMixinFactory.create(scope)

    override val showAddAccountTypeChooser = MutableLiveData<Event<AddAccountLauncherMixin.AddAccountTypePayload>>()

    override val showImportTypeChooser: LiveData<Event<ImportTypeChooserMixin.Payload>> = importTypeChooserMixin.showChooserEvent

    private fun importTypeSelected(chainAccountPayload: AddAccountPayload.ChainAccount, secretType: SecretType) {
        router.openImportAccountScreen(ImportAccountPayload(secretType.asImportType(), chainAccountPayload))
    }

    override fun initiateLaunch(chain: Chain, metaAccount: MetaAccount) {
        when (metaAccount.type) {
            LightMetaAccount.Type.SECRETS -> launchAddFromSecrets(chain, metaAccount)
            LightMetaAccount.Type.WATCH_ONLY -> launchAddWatchOnly(chain, metaAccount)
            LightMetaAccount.Type.LEDGER_LEGACY -> launchAddLedger(chain, metaAccount)

            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.POLKADOT_VAULT,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.MULTISIG,
            LightMetaAccount.Type.PROXIED -> Unit
        }
    }

    private fun launchAddLedger(chain: Chain, metaAccount: MetaAccount) {
        val chainAccountPayload = AddAccountPayload.ChainAccount(chain.id, metaAccount.id)

        router.openAddLedgerChainAccountFlow(chainAccountPayload)
    }

    private fun launchAddWatchOnly(chain: Chain, metaAccount: MetaAccount) {
        cloudBackupChangingWarningMixin.launchChangingConfirmationIfNeeded {
            val chainAccountPayload = AddAccountPayload.ChainAccount(chain.id, metaAccount.id)

            router.openChangeWatchAccount(chainAccountPayload)
        }
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

    private fun addAccountSelected(payload: AddAccountPayload.ChainAccount) {
        scope.launch {
            if (cloudBackupService.session.isSyncWithCloudEnabled()) {
                cloudBackupChangingWarningMixin.launchChangingConfirmationIfNeeded {
                    addAccountWithRecommendedSettings(payload)
                }
            } else {
                router.openMnemonicScreen(accountName = null, payload)
            }
        }
    }

    private fun addAccountWithRecommendedSettings(payload: AddAccountPayload.ChainAccount) {
        scope.launch {
            withContext(Dispatchers.Default) {
                addAccountInteractor.createMetaAccountWithRecommendedSettings(AddAccountType.ChainAccount(payload.chainId, payload.metaId))
            }
        }
    }

    private fun importAccountSelected(chainAccountPayload: AddAccountPayload.ChainAccount) {
        val payload = ImportTypeChooserMixin.Payload(
            onChosen = {
                cloudBackupChangingWarningMixin.launchChangingConfirmationIfNeeded {
                    importTypeSelected(chainAccountPayload, it)
                }
            }
        )
        importTypeChooserMixin.showChooser(payload)
    }
}

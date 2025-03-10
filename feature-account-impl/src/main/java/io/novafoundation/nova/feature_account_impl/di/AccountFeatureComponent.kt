package io.novafoundation.nova.feature_account_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationCommunicator
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.di.modules.ExportModule
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.di.AdvancedEncryptionComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.details.di.AccountDetailsComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates.di.DelegatedAccountUpdatesComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting.di.SelectMultipleWalletsComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress.di.SelectAddressComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.switching.di.SwitchWalletComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.management.di.WalletManagmentComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.changePassword.di.ChangeBackupPasswordComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.di.CreateWalletBackupPasswordComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.syncWallets.di.SyncWalletsBackupPasswordComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.confirmPassword.di.CheckCloudBackupPasswordComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restoreBackup.di.RestoreCloudBackupComponent
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restorePassword.di.RestoreCloudBackupPasswordComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.ShareCompletedReceiver
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.di.ExportJsonComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.di.ExportSeedComponent
import io.novafoundation.nova.feature_account_impl.presentation.importing.di.ImportAccountComponent
import io.novafoundation.nova.feature_account_impl.presentation.language.di.LanguagesComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.di.ManualBackupSelectAccountComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.advanced.di.ManualBackupAdvancedSecretsComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.main.di.ManualBackupSecretsComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.wallets.di.ManualBackupSelectWalletComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning.di.ManualBackupWarningComponent
import io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet.di.SelectWalletComponent
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.di.BackupMnemonicComponent
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.di.ConfirmMnemonicComponent
import io.novafoundation.nova.feature_account_impl.presentation.node.add.di.AddNodeComponent
import io.novafoundation.nova.feature_account_impl.presentation.node.details.di.NodeDetailsComponent
import io.novafoundation.nova.feature_account_impl.presentation.node.list.di.NodesComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.finish.di.FinishImportParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview.di.PreviewImportParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan.di.ScanImportParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start.di.StartImportParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.di.ScanSignParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.di.ShowSignParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.pincode.di.PinCodeComponent
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.di.StartCreateWalletComponent
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change.di.ChangeWatchAccountComponent
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create.di.CreateWatchWalletComponent
import io.novafoundation.nova.feature_cloud_backup_api.di.CloudBackupFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_ledger_core.di.LedgerCoreApi
import io.novafoundation.nova.feature_proxy_api.di.ProxyFeatureApi
import io.novafoundation.nova.feature_swap_core_api.di.SwapCoreApi
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.web3names.di.Web3NamesApi

@Component(
    dependencies = [
        AccountFeatureDependencies::class,
    ],
    modules = [
        AccountFeatureModule::class,
        ExportModule::class
    ]
)
@FeatureScope
interface AccountFeatureComponent : AccountFeatureApi {

    fun advancedEncryptionComponentFactory(): AdvancedEncryptionComponent.Factory

    fun importAccountComponentFactory(): ImportAccountComponent.Factory

    fun backupMnemonicComponentFactory(): BackupMnemonicComponent.Factory

    fun createWalletBackupPasswordFactory(): CreateWalletBackupPasswordComponent.Factory

    fun syncWalletsBackupPasswordFactory(): SyncWalletsBackupPasswordComponent.Factory

    fun changeBackupPasswordComponentFactory(): ChangeBackupPasswordComponent.Factory

    fun restoreCloudBackupFactory(): RestoreCloudBackupComponent.Factory

    fun checkCloudBackupPasswordFactory(): CheckCloudBackupPasswordComponent.Factory

    fun restoreCloudBackupPasswordFactory(): RestoreCloudBackupPasswordComponent.Factory

    fun pincodeComponentFactory(): PinCodeComponent.Factory

    fun confirmMnemonicComponentFactory(): ConfirmMnemonicComponent.Factory

    fun walletManagmentComponentFactory(): WalletManagmentComponent.Factory

    fun switchWalletComponentFactory(): SwitchWalletComponent.Factory

    fun selectWalletComponentFactory(): SelectWalletComponent.Factory

    fun selectAddressComponentFactory(): SelectAddressComponent.Factory

    fun selectMultipleWalletsComponentFactory(): SelectMultipleWalletsComponent.Factory

    fun delegatedAccountUpdatesFactory(): DelegatedAccountUpdatesComponent.Factory

    fun accountDetailsComponentFactory(): AccountDetailsComponent.Factory

    fun connectionsComponentFactory(): NodesComponent.Factory

    fun nodeDetailsComponentFactory(): NodeDetailsComponent.Factory

    fun languagesComponentFactory(): LanguagesComponent.Factory

    fun addNodeComponentFactory(): AddNodeComponent.Factory

    fun exportSeedFactory(): ExportSeedComponent.Factory

    fun exportJsonPasswordFactory(): ExportJsonComponent.Factory

    fun inject(receiver: ShareCompletedReceiver)

    fun createWatchOnlyComponentFactory(): CreateWatchWalletComponent.Factory
    fun changeWatchAccountComponentFactory(): ChangeWatchAccountComponent.Factory

    fun startImportParitySignerComponentFactory(): StartImportParitySignerComponent.Factory
    fun scanImportParitySignerComponentFactory(): ScanImportParitySignerComponent.Factory
    fun previewImportParitySignerComponentFactory(): PreviewImportParitySignerComponent.Factory
    fun finishImportParitySignerComponentFactory(): FinishImportParitySignerComponent.Factory

    fun showSignParitySignerComponentFactory(): ShowSignParitySignerComponent.Factory
    fun scanSignParitySignerComponentFactory(): ScanSignParitySignerComponent.Factory

    fun startCreateWallet(): StartCreateWalletComponent.Factory

    fun manualBackupSelectWallet(): ManualBackupSelectWalletComponent.Factory

    fun manualBackupWarning(): ManualBackupWarningComponent.Factory

    fun manualBackupSecrets(): ManualBackupSecretsComponent.Factory

    fun manualBackupSelectAccount(): ManualBackupSelectAccountComponent.Factory

    fun manualBackupAdvancedSecrets(): ManualBackupAdvancedSecretsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance accountRouter: AccountRouter,
            @BindsInstance polkadotVaultSignInterScreenCommunicator: PolkadotVaultVariantSignCommunicator,
            @BindsInstance ledgerSignInterScreenCommunicator: LedgerSignCommunicator,
            @BindsInstance selectAddressCommunicator: SelectAddressCommunicator,
            @BindsInstance selectMultipleWalletsCommunicator: SelectMultipleWalletsCommunicator,
            @BindsInstance selectWalletCommunicator: SelectWalletCommunicator,
            @BindsInstance pinCodeTwoFactorVerificationCommunicator: PinCodeTwoFactorVerificationCommunicator,
            @BindsInstance syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
            @BindsInstance changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
            @BindsInstance restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
            deps: AccountFeatureDependencies
        ): AccountFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            CurrencyFeatureApi::class,
            ProxyFeatureApi::class,
            DbApi::class,
            VersionsFeatureApi::class,
            Web3NamesApi::class,
            LedgerCoreApi::class,
            CloudBackupFeatureApi::class,
            SwapCoreApi::class
        ]
    )
    interface AccountFeatureDependenciesComponent : AccountFeatureDependencies
}

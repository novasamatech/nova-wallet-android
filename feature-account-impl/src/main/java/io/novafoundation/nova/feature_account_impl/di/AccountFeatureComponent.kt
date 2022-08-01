package io.novafoundation.nova.feature_account_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.di.modules.ExportModule
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.di.AdvancedEncryptionComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.create.di.CreateAccountComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.details.di.AccountDetailsComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.di.AccountListComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.select.di.AccountSwitchComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ShareCompletedReceiver
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.di.ExportJsonConfirmComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password.di.ExportJsonPasswordComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.di.ExportSeedComponent
import io.novafoundation.nova.feature_account_impl.presentation.importing.di.ImportAccountComponent
import io.novafoundation.nova.feature_account_impl.presentation.language.di.LanguagesComponent
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.di.BackupMnemonicComponent
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.di.ConfirmMnemonicComponent
import io.novafoundation.nova.feature_account_impl.presentation.node.add.di.AddNodeComponent
import io.novafoundation.nova.feature_account_impl.presentation.node.details.di.NodeDetailsComponent
import io.novafoundation.nova.feature_account_impl.presentation.node.list.di.NodesComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview.di.PreviewImportParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan.di.ScanImportParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start.di.StartImportParitySignerComponent
import io.novafoundation.nova.feature_account_impl.presentation.pincode.di.PinCodeComponent
import io.novafoundation.nova.feature_account_impl.presentation.settings.di.SettingsComponent
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change.di.ChangeWatchAccountComponent
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create.di.CreateWatchWalletComponent
import io.novafoundation.nova.runtime.di.RuntimeApi

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

    fun createAccountComponentFactory(): CreateAccountComponent.Factory

    fun advancedEncryptionComponentFactory(): AdvancedEncryptionComponent.Factory

    fun importAccountComponentFactory(): ImportAccountComponent.Factory

    fun backupMnemonicComponentFactory(): BackupMnemonicComponent.Factory

    fun profileComponentFactory(): SettingsComponent.Factory

    fun pincodeComponentFactory(): PinCodeComponent.Factory

    fun confirmMnemonicComponentFactory(): ConfirmMnemonicComponent.Factory

    fun accountsComponentFactory(): AccountListComponent.Factory

    fun switchAccountComponentFactory(): AccountSwitchComponent.Factory

    fun accountDetailsComponentFactory(): AccountDetailsComponent.Factory

    fun connectionsComponentFactory(): NodesComponent.Factory

    fun nodeDetailsComponentFactory(): NodeDetailsComponent.Factory

    fun languagesComponentFactory(): LanguagesComponent.Factory

    fun addNodeComponentFactory(): AddNodeComponent.Factory

    fun exportSeedFactory(): ExportSeedComponent.Factory

    fun exportJsonPasswordFactory(): ExportJsonPasswordComponent.Factory

    fun exportJsonConfirmFactory(): ExportJsonConfirmComponent.Factory

    fun inject(receiver: ShareCompletedReceiver)

    fun createWatchOnlyComponentFactory(): CreateWatchWalletComponent.Factory
    fun changeWatchAccountComponentFactory(): ChangeWatchAccountComponent.Factory

    fun startImportParitySignerComponentFactory(): StartImportParitySignerComponent.Factory
    fun scanImportParitySignerComponentFactory(): ScanImportParitySignerComponent.Factory
    fun previewImportParitySignerComponentFactory(): PreviewImportParitySignerComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance accountRouter: AccountRouter,
            @BindsInstance advancedEncryptionCommunicator: AdvancedEncryptionCommunicator,
            deps: AccountFeatureDependencies
        ): AccountFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            DbApi::class
        ]
    )
    interface AccountFeatureDependenciesComponent : AccountFeatureDependencies
}

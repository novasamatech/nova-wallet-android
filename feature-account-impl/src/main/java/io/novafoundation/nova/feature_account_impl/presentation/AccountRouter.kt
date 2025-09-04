package io.novafoundation.nova.feature_account_impl.presentation

import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.navigation.PinRequired
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.navigation.SecureRouter
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerStartPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletPayload

interface AccountRouter : SecureRouter, ReturnableRouter {

    fun openWelcomeScreen()

    fun openMain()

    fun openCreatePincode()

    fun openMnemonicScreen(accountName: String?, payload: AddAccountPayload)

    fun openAdvancedSettings(payload: AdvancedEncryptionModePayload)

    fun openConfirmMnemonicOnCreate(confirmMnemonicPayload: ConfirmMnemonicPayload)

    fun openWallets()

    fun openSwitchWallet()

    fun openDelegatedAccountsUpdates()

    fun openNodes()

    fun openCreateWallet(payload: StartCreateWalletPayload)

    fun openWalletDetails(metaId: Long)

    fun openNodeDetails(nodeId: Int)

    fun openAddNode()

    fun openChangeWatchAccount(payload: AddAccountPayload.ChainAccount)

    @PinRequired
    fun getExportMnemonicDelayedNavigation(exportPayload: ExportPayload.ChainAccount): DelayedNavigation

    @PinRequired
    fun getExportSeedDelayedNavigation(exportPayload: ExportPayload.ChainAccount): DelayedNavigation

    @PinRequired
    fun getExportJsonDelayedNavigation(exportPayload: ExportPayload): DelayedNavigation

    fun exportJsonAction(exportPayload: ExportPayload)

    fun openImportAccountScreen(payload: ImportAccountPayload)

    fun openImportOptionsScreen()

    fun returnToWallet()

    fun finishExportFlow()

    fun openScanImportParitySigner(payload: ParitySignerStartPayload)

    fun openPreviewImportParitySigner(payload: ParitySignerAccountPayload)

    fun openFinishImportParitySigner(payload: ParitySignerAccountPayload)

    fun openScanParitySignerSignature(payload: ScanSignParitySignerPayload)

    fun finishParitySignerFlow()

    fun openAddLedgerChainAccountFlow(addAccountPayload: AddAccountPayload.ChainAccount)

    fun openCreateCloudBackupPassword(walletName: String)

    fun restoreCloudBackup()

    fun openSyncWalletsBackupPassword()

    fun openChangeBackupPasswordFlow()

    fun openRestoreBackupPassword()

    fun openChangeBackupPassword()

    fun openManualBackupSelectAccount(metaId: Long)

    fun openManualBackupConditions(payload: ManualBackupCommonPayload)

    fun openManualBackupSecrets(payload: ManualBackupCommonPayload)

    fun openManualBackupAdvancedSecrets(payload: ManualBackupCommonPayload)

    fun openChainAddressSelector(chainId: String, accountId: ByteArray)

    fun closeChainAddressesSelector()

    fun finishApp()

    fun openAddGenericEvmAddressSelectLedger(metaId: Long)
}

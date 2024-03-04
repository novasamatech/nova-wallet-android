package io.novafoundation.nova.feature_account_impl.presentation

import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.navigation.PinRequired
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.navigation.SecureRouter
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.feature_account_api.presenatation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_api.presenatation.exporting.json.ExportJsonConfirmPayload
import io.novafoundation.nova.feature_account_api.presenatation.mnemonic.confirm.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect.ParitySignerStartPayload
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.sign.scan.ScanSignParitySignerPayload

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

    fun openAddAccount(payload: AddAccountPayload)

    fun openWalletDetails(metaId: Long)

    fun openNodeDetails(nodeId: Int)

    fun openAddNode()

    fun openChangeWatchAccount(payload: AddAccountPayload.ChainAccount)

    @PinRequired
    fun exportMnemonicAction(exportPayload: ExportPayload): DelayedNavigation

    @PinRequired
    fun exportSeedAction(exportPayload: ExportPayload): DelayedNavigation

    @PinRequired
    fun exportJsonPasswordAction(exportPayload: ExportPayload): DelayedNavigation

    fun openExportJsonConfirm(payload: ExportJsonConfirmPayload)

    fun openImportAccountScreen(payload: ImportAccountPayload)

    fun returnToWallet()

    fun finishExportFlow()

    fun openScanImportParitySigner(payload: ParitySignerStartPayload)

    fun openPreviewImportParitySigner(payload: ParitySignerAccountPayload)

    fun openFinishImportParitySigner(payload: ParitySignerAccountPayload)

    fun openScanParitySignerSignature(payload: ScanSignParitySignerPayload)

    fun finishParitySignerFlow()

    fun openAddLedgerChainAccountFlow(payload: AddAccountPayload.ChainAccount)

    fun finishApp()
}

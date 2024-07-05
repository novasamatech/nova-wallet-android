package io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.fixedSelectionOf
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.displayDialogOrNothing
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixin
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.common.utils.progress.startProgress
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapCheckBackupAvailableFailureToUi
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import io.novafoundation.nova.feature_onboarding_api.domain.OnboardingInteractor
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.model.HardwareWalletModel
import kotlinx.coroutines.launch

class ImportWalletOptionsViewModel(
    private val resourceManager: ResourceManager,
    private val router: OnboardingRouter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val onboardingInteractor: OnboardingInteractor,
    private val progressDialogMixinFactory: ProgressDialogMixinFactory,
    customDialogProvider: CustomDialogDisplayer.Presentation,
    cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory,
) : BaseViewModel(), CustomDialogDisplayer.Presentation by customDialogProvider {

    val progressDialogMixin = progressDialogMixinFactory.create()

    val cloudBackupChangingWarningMixin = cloudBackupChangingWarningMixinFactory.create(this)

    val selectHardwareWallet = actionAwaitableMixinFactory.fixedSelectionOf<HardwareWalletModel>()

    val showImportViaCloudButton = flowOf { onboardingInteractor.isCloudBackupAvailableForImport() }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun importMnemonicClicked() {
        openImportType(ImportType.Mnemonic())
    }

    fun importCloudClicked() = launch {
        progressDialogMixin.startProgress(R.string.loocking_backup_progress) {
            onboardingInteractor.checkCloudBackupIsExist()
                .onSuccess { isCloudBackupExist ->
                    if (isCloudBackupExist) {
                        router.restoreCloudBackup()
                    } else {
                        showBackupNotFoundError()
                    }
                }.onFailure {
                    val payload = mapCheckBackupAvailableFailureToUi(resourceManager, it, ::initSignIn)
                    displayDialogOrNothing(payload)
                }
        }
    }

    fun importHardwareClicked() {
        cloudBackupChangingWarningMixin.launchConfirmationIfNeeded {
            launch {
                when (val selection = selectHardwareWallet.awaitAction()) {
                    HardwareWalletModel.LedgerNanoX -> router.openStartImportLedger()

                    is HardwareWalletModel.PolkadotVault -> when (selection.variant) {
                        PolkadotVaultVariant.POLKADOT_VAULT -> router.openStartImportPolkadotVault()
                        PolkadotVaultVariant.PARITY_SIGNER -> router.openStartImportParitySigner()
                    }
                }
            }
        }
    }

    fun importWatchOnlyClicked() {
        cloudBackupChangingWarningMixin.launchConfirmationIfNeeded {
            router.openCreateWatchWallet()
        }
    }

    fun importRawSeedClicked() {
        openImportType(ImportType.Seed)
    }

    fun importJsonClicked() {
        openImportType(ImportType.Json)
    }

    private fun openImportType(importType: ImportType) {
        cloudBackupChangingWarningMixin.launchConfirmationIfNeeded {
            router.openImportAccountScreen(ImportAccountPayload(importType = importType, addAccountPayload = AddAccountPayload.MetaAccount))
        }
    }

    private fun initSignIn() {
        launch {
            onboardingInteractor.signInToCloud()
        }
    }

    private fun showBackupNotFoundError() {
        showError(
            resourceManager.getString(R.string.import_wallet_cloud_backup_not_found_title),
            resourceManager.getString(R.string.import_wallet_cloud_backup_not_found_subtitle),
        )
    }
}

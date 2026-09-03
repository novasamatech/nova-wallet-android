package io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.WalletCreationMethod
import io.novafoundation.nova.analytics.WalletCreationStep
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.displayDialogOrNothing
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.common.utils.progress.startProgress
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType.Mnemonic.Origin
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapCheckBackupAvailableFailureToUi
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_onboarding_api.domain.OnboardingInteractor
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.SelectHardwareWalletBottomSheet
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
    private val ledgerMigrationTracker: LedgerMigrationTracker,
    private val analyticsService: AnalyticsService,
) : BaseViewModel(), CustomDialogDisplayer.Presentation by customDialogProvider {

    val progressDialogMixin = progressDialogMixinFactory.create()

    val cloudBackupChangingWarningMixin = cloudBackupChangingWarningMixinFactory.create(this)

    val selectHardwareWallet = actionAwaitableMixinFactory.create<SelectHardwareWalletBottomSheet.Payload, HardwareWalletModel>()

    val showImportViaCloudButton = flowOf { onboardingInteractor.isCloudBackupAvailableForImport() }
        .shareInBackground()

    /**
     * Set once the user navigates to the next step of the flow so that leaving the screen afterwards is not reported as abandoning
     */
    private var proceededToNextStep = false

    fun backClicked() {
        router.back()
    }

    override fun onCleared() {
        if (!proceededToNextStep) {
            analyticsService.track(AnalyticsEvent.WalletCreationAbandoned(WalletCreationStep.OTHER))
        }

        super.onCleared()
    }

    fun importMnemonicClicked() {
        analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_MNEMONIC))

        openImportType(ImportType.Mnemonic())
    }

    fun importTrustWalletClicked() {
        analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_MNEMONIC))

        openImportType(ImportType.Mnemonic(origin = Origin.TRUST_WALLET))
    }

    fun importCloudClicked() = launch {
        analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.CLOUD_BACKUP))

        progressDialogMixin.startProgress(R.string.loocking_backup_progress) {
            onboardingInteractor.checkCloudBackupIsExist()
                .onSuccess { isCloudBackupExist ->
                    if (isCloudBackupExist) {
                        proceededToNextStep = true
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
        cloudBackupChangingWarningMixin.launchChangingConfirmationIfNeeded {
            launch {
                val genericLedgerSupported = ledgerMigrationTracker.anyChainSupportsMigrationApp()
                val payload = SelectHardwareWalletBottomSheet.Payload(genericLedgerSupported)

                when (val selection = selectHardwareWallet.awaitAction(payload)) {
                    HardwareWalletModel.LedgerLegacy -> {
                        analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_LEDGER))
                        proceededToNextStep = true
                        router.openStartImportLegacyLedger()
                    }

                    HardwareWalletModel.LedgerGeneric -> {
                        analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_LEDGER))
                        proceededToNextStep = true
                        router.openStartImportGenericLedger()
                    }

                    is HardwareWalletModel.PolkadotVault -> when (selection.variant) {
                        PolkadotVaultVariant.POLKADOT_VAULT -> {
                            analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_POLKADOT_VAULT))
                            proceededToNextStep = true
                            router.openStartImportPolkadotVault()
                        }

                        PolkadotVaultVariant.PARITY_SIGNER -> {
                            analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_PARITY_SIGNER))
                            proceededToNextStep = true
                            router.openStartImportParitySigner()
                        }
                    }
                }
            }
        }
    }

    fun importWatchOnlyClicked() {
        analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_WATCH_ONLY))

        cloudBackupChangingWarningMixin.launchChangingConfirmationIfNeeded {
            proceededToNextStep = true
            router.openCreateWatchWallet()
        }
    }

    fun importRawSeedClicked() {
        analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_SEED))

        openImportType(ImportType.Seed)
    }

    fun importJsonClicked() {
        analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.IMPORT_JSON))

        openImportType(ImportType.Json)
    }

    private fun openImportType(importType: ImportType) {
        cloudBackupChangingWarningMixin.launchChangingConfirmationIfNeeded {
            proceededToNextStep = true
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

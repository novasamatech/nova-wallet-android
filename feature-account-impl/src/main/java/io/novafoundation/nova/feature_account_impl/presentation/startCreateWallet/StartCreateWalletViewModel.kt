package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.displayDialogOrNothing
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.finally
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.startCreateWallet.StartCreateWalletInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletPayload.FlowType
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchExistingCloudBackupAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapPreCreateValidationStatusToUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class CreateWalletState {
    SETUP_NAME,
    CHOOSE_BACKUP_WAY
}

class StartCreateWalletViewModel(
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
    private val startCreateWalletInteractor: StartCreateWalletInteractor,
    private val actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
    private val payload: StartCreateWalletPayload,
    customDialogProvider: CustomDialogDisplayer.Presentation
) : BaseViewModel(),
    ActionBottomSheetLauncher by actionBottomSheetLauncherFactory.create(),
    CustomDialogDisplayer.Presentation by customDialogProvider {

    // Used to cancel the job when the user navigates back
    private var cloudBackupValidationJob: Job? = null

    private val _progressFlow = MutableStateFlow(false)
    val progressFlow: Flow<Boolean> = _progressFlow

    private val _createWalletState = MutableStateFlow(CreateWalletState.SETUP_NAME)
    val createWalletState: Flow<CreateWalletState> = _createWalletState

    val nameInput = MutableStateFlow("")

    val isSyncWithCloudEnabled = flowOf { startCreateWalletInteractor.isSyncWithCloudEnabled() }

    val continueButtonState: Flow<DescriptiveButtonState> = combine(progressFlow, nameInput) { progress, name ->
        when {
            progress -> DescriptiveButtonState.Loading
            name.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.start_create_wallet_enter_wallet_name))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }.shareInBackground()

    val showCloudBackupButton: Flow<Boolean> = createWalletState.map { state ->
        when (state) {
            CreateWalletState.SETUP_NAME -> false
            CreateWalletState.CHOOSE_BACKUP_WAY -> payload.flowType == FlowType.FIRST_WALLET
        }
    }.shareInBackground()

    val showManualBackupButton: Flow<Boolean> = createWalletState.map { state ->
        state == CreateWalletState.CHOOSE_BACKUP_WAY
    }.shareInBackground()

    val titleText: Flow<String> = createWalletState.map {
        when (it) {
            CreateWalletState.SETUP_NAME -> resourceManager.getString(R.string.start_create_wallet_name_your_wallet)
            CreateWalletState.CHOOSE_BACKUP_WAY -> resourceManager.getString(R.string.start_create_wallet_your_wallet_is_ready)
        }
    }.shareInBackground()

    val explanationText: Flow<String> = createWalletState.map {
        when (it) {
            CreateWalletState.SETUP_NAME -> resourceManager.getString(R.string.account_create_name_subtitle_v2_2_0)
            CreateWalletState.CHOOSE_BACKUP_WAY -> resourceManager.getString(R.string.start_create_wallet_backup_ready_explanation)
        }
    }.shareInBackground()

    fun backClicked() {
        if (_createWalletState.value == CreateWalletState.SETUP_NAME) {
            router.back()
        } else {
            cloudBackupValidationJob?.cancel()
            _createWalletState.value = CreateWalletState.SETUP_NAME
        }
    }

    fun confirmNameClicked() {
        launch {
            if (startCreateWalletInteractor.isSyncWithCloudEnabled()) {
                _progressFlow.value = true
                startCreateWalletInteractor.createWalletAndSelect(nameInput.value)
                    .onSuccess { router.openMain() }
                    .onFailure { error -> showError(error) }
                _progressFlow.value = false
            } else {
                _createWalletState.value = CreateWalletState.CHOOSE_BACKUP_WAY
            }
        }
    }

    fun cloudBackupClicked() {
        cloudBackupValidationJob = launch {
            val walletName = nameInput.value
            runCatching {
                _progressFlow.value = true
                val validationResult = startCreateWalletInteractor.validateCanCreateBackup()
                if (validationResult is PreCreateValidationStatus.Ok) {
                    router.openCreateCloudBackupPassword(walletName)
                } else {
                    val payload = mapPreCreateValidationStatusToUi(resourceManager, validationResult, ::userHasExistingBackup, ::initSignIn)
                    displayDialogOrNothing(payload)
                }
            }.finally {
                _progressFlow.value = false
            }
        }
    }

    fun manualBackupClicked() {
        router.openMnemonicScreen(nameInput.value, AddAccountPayload.MetaAccount)
    }

    private fun userHasExistingBackup() {
        launchExistingCloudBackupAction(resourceManager, ::openImportCloudBackup)
    }

    private fun openImportCloudBackup() {
        router.restoreCloudBackup()
    }

    private fun initSignIn() {
        launch {
            startCreateWalletInteractor.signInToCloud()
        }
    }
}

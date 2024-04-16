package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.finally
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.startCreateWallet.StartCreateWalletInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchExistingCloudBackupAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapPreCreateValidationStatusToUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val actionBottomSheetLauncher: ActionBottomSheetLauncher,
) : BaseViewModel(), ActionBottomSheetLauncher by actionBottomSheetLauncher {

    // Used to cancel the job when the user navigates back
    private var cloudBackupValidationJob: Job? = null

    val nameInput = MutableStateFlow("")

    private val _createWalletState = MutableStateFlow(CreateWalletState.SETUP_NAME)
    val createWalletState: Flow<CreateWalletState> = _createWalletState

    val confirmNameButtonState: Flow<DescriptiveButtonState> = nameInput.map { name ->
        if (name.isEmpty()) {
            DescriptiveButtonState.Disabled(resourceManager.getString(R.string.start_create_wallet_enter_wallet_name))
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_confirm))
        }
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

    private val _cloudBackupSyncProgressFlow = MutableStateFlow(false)
    val cloudBackupSyncProgressFlow: Flow<Boolean> = _cloudBackupSyncProgressFlow

    fun backClicked() {
        if (_createWalletState.value == CreateWalletState.SETUP_NAME) {
            router.back()
        } else {
            cloudBackupValidationJob?.cancel()
            _createWalletState.value = CreateWalletState.SETUP_NAME
        }
    }

    fun confirmNameClicked() {
        _createWalletState.value = CreateWalletState.CHOOSE_BACKUP_WAY
    }

    fun cloudBackupClicked() {
        cloudBackupValidationJob = launch {
            val walletName = nameInput.value
            runCatching {
                _cloudBackupSyncProgressFlow.value = true
                val validationResult = startCreateWalletInteractor.validateCanCreateBackup()
                if (validationResult is PreCreateValidationStatus.Ok) {
                    router.openCreateCloudBackupPassword(walletName)
                } else {
                    val error = mapPreCreateValidationStatusToUi(resourceManager, validationResult, ::userHasExistingBackup)
                    error?.let { showError(it) }
                }
            }.finally {
                _cloudBackupSyncProgressFlow.value = false
            }
        }
    }

    fun manualBackupClicked() {
        router.openMnemonicScreen(nameInput.value, AddAccountPayload.MetaAccount)
    }

    private fun userHasExistingBackup() {
        actionBottomSheetLauncher.launchExistingCloudBackupAction(resourceManager, ::openImportCloudBackup)
    }

    private fun openImportCloudBackup() {
        router.restoreCloudBackup()
    }
}

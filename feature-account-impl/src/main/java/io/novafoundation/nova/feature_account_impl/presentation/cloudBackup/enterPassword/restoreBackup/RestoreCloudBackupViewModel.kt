package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restoreBackup

import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.EnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base.EnterCloudBackupPasswordViewModel
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchCorruptedBackupFoundAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapRestoreBackupFailureToUi

class RestoreCloudBackupViewModel(
    private val accountInteractor: AccountInteractor,
    router: AccountRouter,
    resourceManager: ResourceManager,
    interactor: EnterCloudBackupInteractor,
    actionBottomSheetLauncher: ActionBottomSheetLauncher,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : EnterCloudBackupPasswordViewModel(
    router,
    resourceManager,
    interactor,
    actionBottomSheetLauncher,
    actionAwaitableMixinFactory
) {

    override suspend fun continueInternal(password: String) {
        interactor.restoreCloudBackup(password)
            .onSuccess {
                continueBasedOnCodeStatus()
            }.onFailure { throwable ->
                // TODO Antony: Handle CannotApplyNonDestructiveDiff
                val titleAndMessage = mapRestoreBackupFailureToUi(
                    resourceManager,
                    throwable,
                    ::corruptedBackupFound
                )
                titleAndMessage?.let { showError(it) }
            }
    }

    private fun corruptedBackupFound() {
        actionBottomSheetLauncher.launchCorruptedBackupFoundAction(resourceManager, ::confirmCloudBackupDelete)
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (accountInteractor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }
}

package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.confirmPassword

import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.EnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base.EnterCloudBackupPasswordViewModel
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapCheckPasswordFailureToUi

class CheckCloudBackupPasswordViewModel(
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
        interactor.confirmCloudBackupPassword(password)
            .onSuccess {
                openChangePasswordScreen()
            }.onFailure { throwable ->
                val titleAndMessage = mapCheckPasswordFailureToUi(resourceManager, throwable)
                titleAndMessage?.let { showError(it) }
            }
    }

    private fun openChangePasswordScreen() {
        router.openChangeBackupPassword()
    }
}

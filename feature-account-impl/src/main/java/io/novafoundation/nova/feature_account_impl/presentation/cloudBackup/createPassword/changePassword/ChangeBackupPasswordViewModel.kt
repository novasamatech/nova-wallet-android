package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.changePassword

import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordResponder
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base.BackupCreatePasswordViewModel
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapChangePasswordValidationStatusToUi

class ChangeBackupPasswordViewModel(
    router: AccountRouter,
    resourceManager: ResourceManager,
    interactor: CreateCloudBackupPasswordInteractor,
    actionBottomSheetLauncher: ActionBottomSheetLauncher,
    private val changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator
) : BackupCreatePasswordViewModel(
    router,
    resourceManager,
    interactor,
    actionBottomSheetLauncher
) {

    override suspend fun internalContinueClicked(password: String) {
        interactor.changePassword(password)
            .onSuccess {
                changeBackupPasswordCommunicator.respond(ChangeBackupPasswordResponder.Success)
                router.back()
            }.onFailure { throwable ->
                val titleAndMessage = mapChangePasswordValidationStatusToUi(resourceManager, throwable)
                showError(titleAndMessage)
            }
    }
}

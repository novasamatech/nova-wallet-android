package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restorePassword

import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordResponder
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.EnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base.EnterCloudBackupPasswordViewModel
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchCorruptedBackupFoundAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapRestorePasswordFailureToUi

class RestoreCloudBackupPasswordViewModel(
    router: AccountRouter,
    resourceManager: ResourceManager,
    interactor: EnterCloudBackupInteractor,
    actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
) : EnterCloudBackupPasswordViewModel(
    router,
    resourceManager,
    interactor,
    actionBottomSheetLauncherFactory,
    actionAwaitableMixinFactory
) {

    override suspend fun continueInternal(password: String) {
        interactor.restoreCloudBackupPassword(password)
            .onSuccess {
                restoreBackupPasswordCommunicator.respond(RestoreBackupPasswordResponder.Success)
                router.back()
            }.onFailure { throwable ->
                val titleAndMessage = mapRestorePasswordFailureToUi(resourceManager, throwable, ::corruptedBackupFound)
                titleAndMessage?.let { showError(titleAndMessage) }
            }
    }

    private fun corruptedBackupFound() {
        launchCorruptedBackupFoundAction(resourceManager, ::confirmCloudBackupDelete)
    }
}

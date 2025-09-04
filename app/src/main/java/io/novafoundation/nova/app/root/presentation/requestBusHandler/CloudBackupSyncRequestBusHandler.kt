package io.novafoundation.nova.app.root.presentation.requestBusHandler

import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bus.EventBus
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.onEachLatest
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.data.cloudBackup.CLOUD_BACKUP_APPLY_SOURCE
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.collect
import io.novafoundation.nova.feature_account_api.domain.cloudBackup.ApplyLocalSnapshotToCloudBackupUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.isBackupable
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchCloudBackupDestructiveChangesNotApplied
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchCloudBackupDestructiveChangesNotAppliedWithoutRouting
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn

class CloudBackupSyncRequestBusHandler(
    private val rootRouter: RootRouter,
    private val scope: RootScope,
    private val resourceManager: ResourceManager,
    private val metaAccountChangesEventBus: MetaAccountChangesEventBus,
    private val applyLocalSnapshotToCloudBackupUseCase: ApplyLocalSnapshotToCloudBackupUseCase,
    private val accountRepository: AccountRepository,
    private val actionBottomSheetLauncher: ActionBottomSheetLauncher,
    private val automaticInteractionGate: AutomaticInteractionGate,
) : RequestBusHandler {

    override fun observe() {
        metaAccountChangesEventBus.observeEvent()
            .filter { it.shouldTriggerBackupSync() }
            .onEachLatest {
                applyLocalSnapshotToCloudBackupUseCase.applyLocalSnapshotToCloudBackupIfSyncEnabled()
                    .onFailure { showDestructiveChangesNotAppliedDialog() }
            }.launchIn(scope)
    }

    private fun EventBus.SourceEvent<MetaAccountChangesEventBus.Event>.shouldTriggerBackupSync(): Boolean {
        if (source == CLOUD_BACKUP_APPLY_SOURCE) return false

        val potentialTriggers = event.collect(
            onAdd = { it.metaAccountType },
            onStructureChanged = { it.metaAccountType },
            onRemoved = { it.metaAccountType },
            onNameChanged = { it.metaAccountType }
        )

        return potentialTriggers.any { it.isBackupable() }
    }

    private suspend fun showDestructiveChangesNotAppliedDialog() {
        automaticInteractionGate.awaitInteractionAllowed()

        if (accountRepository.hasActiveMetaAccounts()) {
            actionBottomSheetLauncher.launchCloudBackupDestructiveChangesNotApplied(
                resourceManager = resourceManager,
                onReviewClicked = ::onReviewIssueClicked
            )
        } else {
            actionBottomSheetLauncher.launchCloudBackupDestructiveChangesNotAppliedWithoutRouting(resourceManager)
        }
    }

    private fun onReviewIssueClicked() {
        rootRouter.openCloudBackupSettings()
    }
}

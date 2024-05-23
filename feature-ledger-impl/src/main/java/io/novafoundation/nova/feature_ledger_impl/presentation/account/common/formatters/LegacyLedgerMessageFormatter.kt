package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertModel.ActionModel
import io.novafoundation.nova.common.view.AlertView.StylePreset
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LegacyLedgerMessageFormatter(
    private val migrationTracker: LedgerMigrationTracker,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val appLinksProvider: AppLinksProvider,
    private val chainId: ChainId,
    private val showAlerts: Boolean
) : LedgerMessageFormatter {

    private var shouldUseMigrationApp: Boolean? = null
    private val cacheMutex = Mutex()

    override suspend fun appName(): String {
        return if (shouldUseMigrationApp()) {
            resourceManager.getString(R.string.account_ledger_migration_app)
        } else {
            val chain = chainRegistry.getChain(chainId)
            chain.name
        }
    }

    context(Browserable.Presentation)
    override suspend fun alertForKind(messageKind: LedgerMessageFormatter.MessageKind): AlertModel? {
        val shouldShowAlert = showAlerts && shouldUseMigrationApp()
        if (!shouldShowAlert) return null

        return when(messageKind) {
            LedgerMessageFormatter.MessageKind.APP_NOT_OPEN -> AlertModel(
                style = StylePreset.INFO,
                message = resourceManager.getString(R.string.account_ledger_legacy_warning_title),
                subMessage = resourceManager.getString(R.string.account_ledger_legacy_warning_message),
                action = ActionModel(
                    text = resourceManager.getString(R.string.common_find_out_more),
                    listener = { showBrowser(appLinksProvider.ledgerMigrationArticle) }
                )
            )

            LedgerMessageFormatter.MessageKind.OTHER ->  AlertModel(
                style = StylePreset.INFO,
                message = resourceManager.getString(R.string.account_ledger_legacy_warning_title),
                subMessage = resourceManager.getString(R.string.account_ledger_migration_deprecation_message),
                action = ActionModel(
                    text = resourceManager.getString(R.string.common_find_out_more),
                    listener = { showBrowser(appLinksProvider.ledgerMigrationArticle) }
                )
            )
        }
    }

    private suspend fun shouldUseMigrationApp(): Boolean = cacheMutex.withLock {
        if (shouldUseMigrationApp == null) {
            shouldUseMigrationApp = migrationTracker.shouldUseMigrationApp(chainId)
        }

        shouldUseMigrationApp!!
    }
}

package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start.StartImportLedgerViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class StartImportLegacyLedgerViewModel(
    private val resourceManager: ResourceManager,
    private val router: LedgerRouter,
    private val appLinksProvider: AppLinksProvider,
    private val ledgerMigrationTracker: LedgerMigrationTracker,
) : StartImportLedgerViewModel(router, appLinksProvider), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    override fun continueClicked() {
        router.openImportFillWallet()
    }

    val warningModel = flowOf { ledgerMigrationTracker.anyChainSupportsMigrationApp() }
        .map { getWarningModel(it) }
        .onStart { emit(null) }
        .shareInBackground()

    private fun getWarningModel(shouldBeShown: Boolean): AlertModel? {
        if (!shouldBeShown) return null

        return AlertModel(
            style = AlertView.Style.fromPreset(AlertView.StylePreset.WARNING),
            message = resourceManager.getString(R.string.account_ledger_legacy_warning_title),
            subMessage = resourceManager.getString(R.string.account_ledger_legacy_warning_message),
            action = AlertModel.ActionModel(resourceManager.getString(R.string.common_find_out_more), ::deprecationWarningClicked)
        )
    }

    fun deprecationWarningClicked() {
        openBrowserEvent.value = appLinksProvider.ledgerMigrationArticle.event()
    }
}

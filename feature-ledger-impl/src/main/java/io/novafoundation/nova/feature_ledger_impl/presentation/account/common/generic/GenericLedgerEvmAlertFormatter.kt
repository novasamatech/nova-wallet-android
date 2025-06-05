package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.generic

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_ledger_impl.R
import javax.inject.Inject

interface GenericLedgerEvmAlertFormatter {

    fun createUpdateAppToGetEvmAddressAlert(): AlertModel
}

@FeatureScope
class RealGenericLedgerEvmAlertFormatter @Inject constructor(
    private val resourceManager: ResourceManager,
) : GenericLedgerEvmAlertFormatter {

    override fun createUpdateAppToGetEvmAddressAlert(): AlertModel {
        return AlertModel(
            style = AlertView.Style.fromPreset(AlertView.StylePreset.WARNING),
            message = resourceManager.getString(R.string.ledger_select_address_update_for_evm_title),
            subMessage = resourceManager.getString(R.string.ledger_select_address_update_for_evm_message)
        )
    }
}

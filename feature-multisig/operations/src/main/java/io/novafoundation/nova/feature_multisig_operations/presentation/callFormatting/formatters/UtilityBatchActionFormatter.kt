package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
class UtilityBatchActionFormatter @Inject constructor(
    private val assetIconProvider: AssetIconProvider,
    private val resourceManager: ResourceManager,
) : MultisigActionFormatterDelegate {

    override suspend fun formatPreview(
        visit: CallVisit,
        chain: Chain
    ): MultisigActionFormatterDelegatePreviewResult? {
        if (visit.isNotUtilityBatchCall()) return null

        return MultisigActionFormatterDelegatePreviewResult(
            title = resourceManager.getString(R.string.multisig_operation_utility_batch_title),
            subtitle = visit.call.module.name.capitalize(),
            primaryValue = null,
            icon = assetIconProvider.multisigFormatAssetIcon(chain)
        )
    }

    override suspend fun formatDetails(visit: CallVisit, chain: Chain): MultisigActionFormatterDelegateDetailsResult? {
        if (visit.isNotUtilityBatchCall()) return null

        return MultisigActionFormatterDelegateDetailsResult(
            title = resourceManager.getString(R.string.multisig_operation_utility_batch_title),
            primaryAmount = null,
            tableEntries = emptyList()
        )
    }

    override suspend fun formatMessageCall(visit: CallVisit, chain: Chain): String? {
        if (visit.isNotUtilityBatchCall()) return null

        return resourceManager.getString(
            R.string.multisig_operation_default_call_format,
            visit.call.module.name.capitalize(),
            resourceManager.getString(R.string.multisig_operation_utility_batch_title)
        )
    }

    private fun CallVisit.isNotUtilityBatchCall(): Boolean = !isUtilityBatchCall()

    private fun CallVisit.isUtilityBatchCall(): Boolean {
        val module = call.module.name
        val call = call.function.name
        return module == Modules.UTILITY && call == "batch"
    }
}

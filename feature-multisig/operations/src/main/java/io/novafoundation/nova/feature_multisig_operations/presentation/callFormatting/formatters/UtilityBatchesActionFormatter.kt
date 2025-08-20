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
class UtilityBatchesActionFormatter @Inject constructor(
    private val assetIconProvider: AssetIconProvider,
    private val resourceManager: ResourceManager,
) : MultisigActionFormatterDelegate {

    override suspend fun formatPreview(
        visit: CallVisit,
        chain: Chain
    ): MultisigActionFormatterDelegatePreviewResult? {
        val batchCallFormat = visit.formatCall() ?: return null

        return MultisigActionFormatterDelegatePreviewResult(
            title = batchCallFormat,
            subtitle = visit.call.module.name.capitalize(),
            primaryValue = null,
            icon = assetIconProvider.multisigFormatAssetIcon(chain)
        )
    }

    override suspend fun formatDetails(visit: CallVisit, chain: Chain): MultisigActionFormatterDelegateDetailsResult? {
        val batchCallFormat = visit.formatCall() ?: return null

        return MultisigActionFormatterDelegateDetailsResult(
            title = batchCallFormat,
            primaryAmount = null,
            tableEntries = emptyList()
        )
    }

    override suspend fun formatMessageCall(visit: CallVisit, chain: Chain): String? {
        val batchCallFormat = visit.formatCall() ?: return null

        return resourceManager.getString(
            R.string.multisig_operation_default_call_format,
            visit.call.module.name.capitalize(),
            batchCallFormat
        )
    }

    private fun CallVisit.formatCall(): String? {
        if (call.module.name != Modules.UTILITY) return null

        return when (call.function.name) {
            "batch" -> resourceManager.getString(R.string.multisig_operation_utility_batch_title)
            "batch_all" -> resourceManager.getString(R.string.multisig_operation_utility_batch_all_title)
            "force_batch" -> resourceManager.getString(R.string.multisig_operation_utility_force_batch_title)
            else -> null
        }
    }
}

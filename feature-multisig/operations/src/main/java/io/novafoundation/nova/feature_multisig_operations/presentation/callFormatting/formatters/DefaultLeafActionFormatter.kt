package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.splitAndCapitalizeWords
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.isLeaf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject


@FeatureScope
class DefaultLeafActionFormatter @Inject constructor(
    private val assetIconProvider: AssetIconProvider,
    private val resourceManager: ResourceManager,
) : MultisigActionFormatterDelegate {

    override suspend fun formatPreview(
        visit: CallVisit,
        chain: Chain
    ): MultisigActionFormatterDelegatePreviewResult? {
        if (!visit.isLeaf) return null

        return MultisigActionFormatterDelegatePreviewResult(
            title = visit.call.function.name.splitAndCapitalizeWords(),
            subtitle = visit.call.module.name.splitAndCapitalizeWords(),
            primaryValue = null,
            icon = assetIconProvider.multisigFormatAssetIcon(chain)
        )
    }

    override suspend fun formatDetails(visit: CallVisit, chain: Chain): MultisigActionFormatterDelegateDetailsResult? {
        if (!visit.isLeaf) return null

        return MultisigActionFormatterDelegateDetailsResult(
            title = "${visit.call.module.name}.${visit.call.function.name}",
            primaryAmount = null,
            tableEntries = emptyList()
        )
    }

    override suspend fun formatMessageCall(visit: CallVisit, chain: Chain): String? {
        if (!visit.isLeaf) return null

        return resourceManager.getString(
            R.string.multisig_operation_default_call_format,
            visit.call.module.name.capitalize(),
            visit.call.function.name.capitalize()
        )
    }
}

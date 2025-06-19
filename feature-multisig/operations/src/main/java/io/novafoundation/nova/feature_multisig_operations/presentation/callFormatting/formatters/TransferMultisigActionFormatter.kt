package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.tryParseTransfer
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import javax.inject.Inject

@FeatureScope
class TransferMultisigActionFormatter @Inject constructor(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val resourceManager: ResourceManager,
): MultisigActionFormatterDelegate {

    override suspend fun formatAction(
        visit: CallVisit,
        chain: Chain
    ): MultisigActionFormatterDelegateResult? {
        val parsedTransfer = assetSourceRegistry.allSources().tryFindNonNull {
            it.transfers.tryParseTransfer(visit.call, chain)
        } ?: return null

        val destAddress = chain.addressOf(parsedTransfer.destination)

        return MultisigActionFormatterDelegateResult(
            title = resourceManager.getString(R.string.transfer_title),
            subtitle = resourceManager.getString(R.string.transfer_history_send_to, destAddress),
            primaryValue = parsedTransfer.amount.formatPlanks(),
            icon = R.drawable.ic_arrow_up.asIcon()
        )
    }
}

package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalWithOnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.account.identity.getNameOrAddress
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.MultisigActionFormatterDelegateDetailsResult.TableEntry
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.MultisigActionFormatterDelegateDetailsResult.TableValue
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.model.TransferParsedFromCall
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.tryParseTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.toIdWithAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import javax.inject.Inject

@FeatureScope
class TransferMultisigActionFormatter @Inject constructor(
    @LocalWithOnChainIdentity private val identityProvider: IdentityProvider,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val resourceManager: ResourceManager,
) : MultisigActionFormatterDelegate {

    override suspend fun formatPreview(
        visit: CallVisit,
        chain: Chain
    ): MultisigActionFormatterDelegatePreviewResult? {
        val parsedTransfer = tryParseTransfer(visit, chain) ?: return null

        val destAddress = chain.addressOf(parsedTransfer.destination)

        return MultisigActionFormatterDelegatePreviewResult(
            title = resourceManager.getString(R.string.transfer_title),
            subtitle = resourceManager.getString(R.string.transfer_history_send_to, destAddress),
            primaryValue = parsedTransfer.amount.formatPlanks(),
            icon = R.drawable.ic_arrow_up.asIcon()
        )
    }

    override suspend fun formatDetails(visit: CallVisit, chain: Chain): MultisigActionFormatterDelegateDetailsResult? {
        val parsedTransfer = tryParseTransfer(visit, chain) ?: return null

        return MultisigActionFormatterDelegateDetailsResult(
            title = resourceManager.getString(R.string.transfer_title),
            primaryAmount = parsedTransfer.amount.toIdWithAmount(),
            tableEntries = listOf(
                TableEntry(
                    name = resourceManager.getString(R.string.wallet_recipient),
                    value = TableValue.Account(parsedTransfer.destination, chain)
                )
            )
        )
    }

    override suspend fun formatMessageCall(visit: CallVisit, chain: Chain): String? {
        val parsedTransfer = tryParseTransfer(visit, chain) ?: return null

        val accountName = identityProvider.getNameOrAddress(parsedTransfer.destination, chain)
        val formattedAmount = parsedTransfer.amount.formatPlanks()

        return resourceManager.getString(
            R.string.multisig_transaction_message_transfer,
            formattedAmount,
            accountName
        )
    }

    private suspend fun tryParseTransfer(
        visit: CallVisit,
        chain: Chain
    ): TransferParsedFromCall? {
        return assetSourceRegistry.allSources().tryFindNonNull {
            it.transfers.tryParseTransfer(visit.call, chain)
        }
    }
}

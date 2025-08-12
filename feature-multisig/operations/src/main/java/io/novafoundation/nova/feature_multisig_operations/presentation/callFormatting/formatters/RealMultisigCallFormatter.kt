package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.utils.splitAndCapitalizeWords
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallDetailsModel
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallPreviewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallTraversal
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.collect
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.isLeaf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import javax.inject.Inject

@FeatureScope
class RealMultisigCallFormatter @Inject constructor(
    private val delegates: Set<@JvmSuppressWildcards MultisigActionFormatterDelegate>,
    private val resourceManager: ResourceManager,
    private val callTraversal: CallTraversal,
    @LocalIdentity private val identityProvider: IdentityProvider,
    private val addressIconGenerator: AddressIconGenerator,
    private val assetIconProvider: AssetIconProvider,
    private val tokenUseCase: ArbitraryTokenUseCase,
) : MultisigCallFormatter {

    override suspend fun formatPreview(
        call: GenericCall.Instance?,
        initialOrigin: AccountIdKey,
        chain: Chain
    ): MultisigCallPreviewModel {
        return formatCall(
            call = call,
            initialOrigin = initialOrigin,
            chain = chain,
            formatUnknown = ::formatUnknownPreview,
            formatDefault = { formatDefaultPreview(it, chain) },
            formatSpecific = { delegate, callVisit -> delegate.formatPreview(callVisit, chain) },
            constructFinalResult = { delegateResult, onBehalfOf -> createCallPreview(delegateResult, onBehalfOf) }
        )
    }

    override suspend fun formatDetails(
        call: GenericCall.Instance?,
        initialOrigin: AccountIdKey,
        chain: Chain
    ): MultisigCallDetailsModel {
        return formatCall(
            call = call,
            initialOrigin = initialOrigin,
            chain = chain,
            formatUnknown = ::formatUnknownDetails,
            formatDefault = { formatDefaultDetails(it) },
            formatSpecific = { delegate, callVisit -> delegate.formatDetails(callVisit, chain) },
            constructFinalResult = { delegateResult, onBehalfOf -> createCallDetails(delegateResult, onBehalfOf) }
        )
    }

    override suspend fun formatPushNotificationMessage(
        call: GenericCall.Instance?,
        initialOrigin: AccountIdKey,
        chain: Chain
    ): String {
        return formatCall(
            call = call,
            initialOrigin = initialOrigin,
            chain = chain,
            formatUnknown = { formatUnknownPushNotification(chain) },
            formatDefault = { formatDefaultPushNotification(chain, it) },
            formatSpecific = { delegate, callVisit -> delegate.formatPushNotificationMessage(callVisit, chain) },
            constructFinalResult = { delegateResult, _ -> delegateResult }
        )
    }

    override suspend fun formatExecutedOperationMessage(
        call: GenericCall.Instance?,
        initialOrigin: AccountIdKey,
        chain: Chain
    ): String {
        return formatCall(
            call = call,
            initialOrigin = initialOrigin,
            chain = chain,
            formatUnknown = { formatUnknownCallExecutedMessage(chain) },
            formatDefault = { formatDefaultExecutedMessage(chain, it) },
            formatSpecific = { delegate, callVisit -> delegate.formatExecutedMessage(callVisit, chain) },
            constructFinalResult = { delegateResult, _ -> delegateResult }
        )
    }

    override suspend fun formatRejectedOperationMessage(
        call: GenericCall.Instance?,
        initialOrigin: AccountIdKey,
        rejectedAccountName: String,
        chain: Chain
    ): String {
        return formatCall(
            call = call,
            initialOrigin = initialOrigin,
            chain = chain,
            formatUnknown = { formatUnknownCallRejectedMessage(chain, rejectedAccountName) },
            formatDefault = { formatDefaultRejectedMessage(chain, it, rejectedAccountName) },
            formatSpecific = { delegate, callVisit -> delegate.formatRejectedMessage(callVisit, chain, rejectedAccountName) },
            constructFinalResult = { delegateResult, _ -> delegateResult }
        )
    }

    private suspend fun <D : Any, R> formatCall(
        call: GenericCall.Instance?,
        initialOrigin: AccountIdKey,
        chain: Chain,
        formatUnknown: () -> R,
        formatDefault: suspend (GenericCall.Instance) -> R,
        formatSpecific: suspend (MultisigActionFormatterDelegate, CallVisit) -> D?,
        constructFinalResult: suspend (D, onBehalfOf: AddressModel?) -> R
    ): R {
        if (call == null) return formatUnknown()

        val singleFormattedVisit = callTraversal.collect(call, initialOrigin)
            .map { formatCallVisit(it) { delegate -> formatSpecific(delegate, it) } }
            .ensureSingleFormattedVisit()

        return if (singleFormattedVisit != null) {
            val (singleMatch, singleMatchVisit) = singleFormattedVisit

            val onBehalfOf = createOnBehalfOf(singleMatchVisit, initialOrigin, chain)
            return constructFinalResult(singleMatch!!, onBehalfOf)
        } else {
            formatDefault(call)
        }
    }

    private fun <T> List<DelegateResultWithVisit<T>>.ensureSingleFormattedVisit(): DelegateResultWithVisit<T>? {
        // We do not want to present a formatted call when there was at least one not resolved leaf - it might make signers not notice unformatted part whatsoever
        // So, we always forbid formatting if some leaf was not resolved
        val hasUnformattedLeafs = any { (formatResult, callVisit) -> formatResult == null && callVisit.isLeaf }
        // We do not have a meaningful way to concat multiple formatted visits, so we only format when there is only single successfully one
        val successfullyFormattedVisits = filter { it.first != null }

        val canShowFormattedResult = !hasUnformattedLeafs && successfullyFormattedVisits.size == 1

        return if (canShowFormattedResult) {
            successfullyFormattedVisits.single()
        } else {
            null
        }
    }

    private fun createCallPreview(
        delegateResult: MultisigActionFormatterDelegatePreviewResult,
        onBehalfOf: AddressModel?
    ): MultisigCallPreviewModel {
        return with(delegateResult) { MultisigCallPreviewModel(title, subtitle, primaryValue, icon, onBehalfOf) }
    }

    private suspend fun createCallDetails(
        delegateResult: MultisigActionFormatterDelegateDetailsResult,
        onBehalfOf: AddressModel?
    ): MultisigCallDetailsModel {
        return MultisigCallDetailsModel(
            title = delegateResult.title,
            primaryAmount = delegateResult.primaryAmount?.let {
                val token = tokenUseCase.getToken(it.chainAssetId)
                mapAmountToAmountModel(it.amount, token)
            },
            tableEntries = delegateResult.tableEntries.map { it.toUi() },
            onBehalfOf = onBehalfOf
        )
    }

    private suspend fun MultisigActionFormatterDelegateDetailsResult.TableEntry.toUi(): MultisigCallDetailsModel.TableEntry {
        return MultisigCallDetailsModel.TableEntry(
            name = name,
            value = value.toUi()
        )
    }

    private suspend fun MultisigActionFormatterDelegateDetailsResult.TableValue.toUi(): MultisigCallDetailsModel.TableValue {
        return when (this) {
            is MultisigActionFormatterDelegateDetailsResult.TableValue.Account -> {
                val addressModel = addressIconGenerator.createAccountAddressModel(
                    chain = chain,
                    accountId = accountId.value,
                    name = identityProvider.identityFor(accountId.value, chain.id)?.name
                )

                MultisigCallDetailsModel.TableValue.Account(addressModel, chain)
            }
        }
    }

    private suspend fun createOnBehalfOf(
        callVisit: CallVisit,
        initialOrigin: AccountIdKey,
        chain: Chain
    ): AddressModel? {
        if (callVisit.callOrigin == initialOrigin) return null

        val onBehalfOf = callVisit.callOrigin.value

        return addressIconGenerator.createAccountAddressModel(
            chain = chain,
            accountId = onBehalfOf,
            name = identityProvider.identityFor(onBehalfOf, chain.id)?.name
        )
    }

    private suspend fun <D : Any> formatCallVisit(
        callVisit: CallVisit,
        format: suspend (MultisigActionFormatterDelegate) -> D?
    ): DelegateResultWithVisit<D> {
        val result = delegates.tryFindNonNull { format(it) }
        return result to callVisit
    }

    private fun formatDefaultPreview(call: GenericCall.Instance, chain: Chain): MultisigCallPreviewModel {
        return MultisigCallPreviewModel(
            title = call.function.name.splitAndCapitalizeWords(),
            subtitle = call.module.name.splitAndCapitalizeWords(),
            primaryValue = null,
            icon = assetIconProvider.getAssetIconOrFallback(chain.utilityAsset, AssetIconMode.WHITE),
            onBehalfOf = null
        )
    }

    private fun formatUnknownPreview(): MultisigCallPreviewModel {
        return MultisigCallPreviewModel(
            title = resourceManager.getString(R.string.multisig_operations_unknown_calldata),
            subtitle = null,
            primaryValue = null,
            icon = R.drawable.ic_unknown_operation.asIcon(),
            onBehalfOf = null
        )
    }

    private fun formatDefaultDetails(call: GenericCall.Instance): MultisigCallDetailsModel {
        return MultisigCallDetailsModel(
            title = "${call.module.name}.${call.function.name}",
            primaryAmount = null,
            tableEntries = emptyList(),
            onBehalfOf = null
        )
    }

    private fun formatUnknownDetails(): MultisigCallDetailsModel {
        return MultisigCallDetailsModel(
            title = resourceManager.getString(R.string.multisig_operations_unknown_calldata),
            primaryAmount = null,
            tableEntries = emptyList(),
            onBehalfOf = null
        )
    }

    private fun formatUnknownPushNotification(chain: Chain): String {
        return resourceManager.getString(
            R.string.multisig_notification_init_transaction_message,
            resourceManager.getString(R.string.multisig_operations_unknown_calldata),
            chain.name
        )
    }

    private fun formatDefaultPushNotification(chain: Chain, call: GenericCall.Instance): String {
        return resourceManager.getString(
            R.string.multisig_notification_init_transaction_message,
            call.format(),
            chain.name
        )
    }

    private fun formatUnknownCallExecutedMessage(chain: Chain): String {
        return resourceManager.getString(
            R.string.multisig_transaction_executed_dialog_message,
            resourceManager.getString(R.string.multisig_transaction_dialog_message_unknown_call),
            chain.name
        )
    }

    private fun formatUnknownCallRejectedMessage(chain: Chain, rejectedAccountName: String): String {
        return resourceManager.getString(
            R.string.multisig_transaction_rejected_dialog_message,
            resourceManager.getString(R.string.multisig_transaction_dialog_message_unknown_call),
            chain.name,
            rejectedAccountName
        )
    }

    private fun formatDefaultExecutedMessage(chain: Chain, call: GenericCall.Instance): String {
        return resourceManager.getString(
            R.string.multisig_transaction_rejected_dialog_message,
            call.format(),
            chain.name
        )
    }

    private fun formatDefaultRejectedMessage(chain: Chain, call: GenericCall.Instance, rejectedAccountName: String): String {
        return resourceManager.getString(
            R.string.multisig_transaction_rejected_dialog_message,
            call.format(),
            chain.name,
            rejectedAccountName
        )
    }

    private fun GenericCall.Instance.format(): String {
        return "${module.name.capitalize()} ${function.name.capitalize()}"
    }
}

private typealias DelegateResultWithVisit<T> = Pair<T?, CallVisit>

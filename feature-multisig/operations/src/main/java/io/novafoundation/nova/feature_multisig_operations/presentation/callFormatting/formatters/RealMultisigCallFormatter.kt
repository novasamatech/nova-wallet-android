package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.utils.splitAndCapitalizeWords
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallPreviewModel
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallTraversal
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.collect
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
) : MultisigCallFormatter {

    override suspend fun formatMultisigCall(
        call: GenericCall.Instance?,
        initialOrigin: AccountIdKey,
        chain: Chain
    ): MultisigCallPreviewModel {
        if (call == null) return formatUnknown()

        val formatted = callTraversal.collect(call, initialOrigin)
            .mapNotNull { formatCallVisit(it, chain) }

        return if (formatted.size == 1) {
            val (singleMatch, singleMatchVisit) = formatted.single()
            createCallPreview(singleMatchVisit, singleMatch, initialOrigin, chain)
        } else {
            formatDefault(call, chain)
        }
    }

    private suspend fun createCallPreview(
        callVisit: CallVisit,
        delegateResult: MultisigActionFormatterDelegateResult,
        initialOrigin: AccountIdKey,
        chain: Chain
    ): MultisigCallPreviewModel {
        val onBehalfOf = createOnBehalfOf(callVisit, initialOrigin, chain)
        return with(delegateResult) { MultisigCallPreviewModel(title, subtitle, primaryValue, icon, onBehalfOf) }
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

    private suspend fun formatCallVisit(
        callVisit: CallVisit,
        chain: Chain,
    ): DelegateResultWithVisit? {
        val result = delegates.tryFindNonNull { it.formatAction(callVisit, chain) } ?: return null
        return result to callVisit
    }

    private fun formatDefault(call: GenericCall.Instance, chain: Chain): MultisigCallPreviewModel {
        return MultisigCallPreviewModel(
            title = call.function.name.splitAndCapitalizeWords(),
            subtitle = call.module.name.splitAndCapitalizeWords(),
            primaryValue = null,
            icon = assetIconProvider.getAssetIconOrFallback(chain.utilityAsset, AssetIconMode.WHITE),
            onBehalfOf = null
        )
    }

    private fun formatUnknown(): MultisigCallPreviewModel {
        return MultisigCallPreviewModel(
            title = resourceManager.getString(R.string.multisig_operations_unknown_calldata),
            subtitle = null,
            primaryValue = null,
            icon = R.drawable.ic_unknown_operation.asIcon(),
            onBehalfOf = null
        )
    }
}

private typealias DelegateResultWithVisit = Pair<MultisigActionFormatterDelegateResult, CallVisit>

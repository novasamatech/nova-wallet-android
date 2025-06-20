package io.novafoundation.nova.feature_account_impl.presentation.proxy.sign

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.feature_account_impl.presentation.sign.NestedSigningPresenter
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@FeatureScope
class RealProxySigningPresenter @Inject constructor(
    private val contextManager: ContextManager,
    private val resourceManager: ResourceManager,
    private val signingNotSupportedPresentable: SigningNotSupportedPresentable,
    private val nestedSigningPresenter: NestedSigningPresenter,
) : ProxySigningPresenter {

    override suspend fun acknowledgeProxyOperation(proxiedMetaAccount: MetaAccount, proxyMetaAccount: MetaAccount): Boolean {
        return nestedSigningPresenter.acknowledgeNestedSignOperation(
            warningShowFor = proxiedMetaAccount,
            title = { resourceManager.getString(R.string.proxy_signing_warning_title) },
            subtitle = { formatSubtitleForWarning(proxyMetaAccount) },
            iconRes = { R.drawable.ic_proxy }
        )
    }

    override suspend fun notEnoughPermission(
        proxiedMetaAccount: MetaAccount,
        proxyMetaAccount: MetaAccount,
        proxyTypes: List<ProxyType>
    ) = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            ProxySignNotEnoughPermissionBottomSheet(
                context = contextManager.getActivity()!!,
                subtitle = formatNotEnoughPermissionWarning(proxiedMetaAccount, proxyMetaAccount, proxyTypes),
                onSuccess = { continuation.resume(Unit) }
            ).show()
        }
    }

    override suspend fun signingIsNotSupported() {
        signingNotSupportedPresentable.presentSigningNotSupported(
            SigningNotSupportedPresentable.Payload(
                iconRes = R.drawable.ic_proxy,
                message = resourceManager.getString(R.string.proxy_signing_is_not_supported_message)
            )
        )
    }

    override suspend fun notEnoughFee(
        metaAccount: MetaAccount,
        chainAsset: Chain.Asset,
        availableBalance: BigInteger,
        fee: BigInteger
    ) {
        nestedSigningPresenter.presentValidationFailure(
            title = resourceManager.getString(R.string.error_not_enough_to_pay_fee_title),
            message = resourceManager.getString(
                R.string.proxy_error_not_enough_to_pay_fee_message,
                metaAccount.name,
                fee.amountFromPlanks(chainAsset.precision).formatTokenAmount(chainAsset.symbol),
                availableBalance.amountFromPlanks(chainAsset.precision).formatTokenAmount(chainAsset.symbol)
            )
        )
    }

    private fun formatSubtitleForWarning(proxyMetaAccount: MetaAccount): CharSequence {
        val subtitle = resourceManager.getString(R.string.proxy_signing_warning_message)
        val primaryColor = resourceManager.getColor(R.color.text_primary)
        val proxyName = proxyMetaAccount.name.toSpannable(colorSpan(primaryColor))
        return SpannableFormatter.format(subtitle, proxyName)
    }

    private fun formatNotEnoughPermissionWarning(
        proxiedMetaAccount: MetaAccount,
        proxyMetaAccount: MetaAccount,
        proxyTypes: List<ProxyType>
    ): CharSequence {
        val primaryColor = resourceManager.getColor(R.color.text_primary)

        val proxiedName = proxiedMetaAccount.name.toSpannable(colorSpan(primaryColor))
        val proxyName = proxyMetaAccount.name.toSpannable(colorSpan(primaryColor))

        return if (proxyTypes.isNotEmpty()) {
            val subtitle = resourceManager.getString(R.string.proxy_signing_not_enough_permission_message)

            val proxyTypesBuffer = SpannableStringBuilder()
            val proxyTypesCharSequence = proxyTypes.joinTo(proxyTypesBuffer) { it.name.toSpannable(colorSpan(primaryColor)) }

            SpannableFormatter.format(subtitle, proxiedName, proxyName, proxyTypesCharSequence)
        } else {
            val subtitle = resourceManager.getString(R.string.proxy_signing_none_permissions_message)
            SpannableFormatter.format(subtitle, proxiedName, proxyName)
        }
    }
}

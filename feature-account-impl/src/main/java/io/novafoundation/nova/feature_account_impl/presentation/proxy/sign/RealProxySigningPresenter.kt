package io.novafoundation.nova.feature_account_impl.presentation.proxy.sign

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val KEY_DONT_SHOW_AGAIN = "proxy_sign_warning_dont_show_again"

class RealProxySigningPresenter(
    private val contextManager: ContextManager,
    private val resourceManager: ResourceManager,
    private val signingNotSupportedPresentable: SigningNotSupportedPresentable,
    private val preferences: Preferences
) : ProxySigningPresenter {

    override suspend fun acknowledgeProxyOperation(proxiedMetaAccount: MetaAccount, proxyMetaAccount: MetaAccount): Boolean = withContext(Dispatchers.Main) {
        if (noNeedToShowWarning(proxiedMetaAccount)) {
            return@withContext true
        }

        val resumingAllowed = suspendCoroutine { continuation ->
            ProxySignWarningBottomSheet(
                context = contextManager.getActivity()!!,
                subtitle = formatSubtitleForWarning(proxyMetaAccount),
                onFinish = {
                    continuation.resume(it)
                },
                dontShowAgain = { dontShowAgain(proxiedMetaAccount) }
            ).show()
        }

        return@withContext resumingAllowed
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
        fee: Fee
    ) = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            dialog(contextManager.getActivity()!!) {
                setTitle(R.string.error_not_enough_to_pay_fee_title)
                setMessage(
                    resourceManager.getString(
                        R.string.proxy_error_not_enough_to_pay_fee_message,
                        metaAccount.name,
                        chainAsset.amountFromPlanks(fee.amount).formatTokenAmount(chainAsset),
                        chainAsset.amountFromPlanks(availableBalance).formatTokenAmount(chainAsset),
                    )
                )

                chainAsset.amountFromPlanks(availableBalance).formatTokenAmount(chainAsset)

                setPositiveButton(io.novafoundation.nova.common.R.string.common_close) { _, _ -> continuation.resume(Unit) }
            }
        }
    }

    private fun noNeedToShowWarning(proxyMetaAccount: MetaAccount): Boolean {
        return preferences.getBoolean(makePrefsKey(proxyMetaAccount), false)
    }

    private fun dontShowAgain(proxyMetaAccount: MetaAccount) {
        preferences.putBoolean(makePrefsKey(proxyMetaAccount), true)
    }

    private fun makePrefsKey(proxyMetaAccount: MetaAccount): String {
        return "${KEY_DONT_SHOW_AGAIN}_${proxyMetaAccount.id}"
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

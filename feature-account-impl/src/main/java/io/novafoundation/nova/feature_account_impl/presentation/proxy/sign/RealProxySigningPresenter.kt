package io.novafoundation.nova.feature_account_impl.presentation.proxy.sign

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val KEY_DONT_SHOW_AGAIN = "proxy_sign_warning_dont_show_again"

class RealProxySigningPresenter(
    private val contextManager: ContextManager,
    private val resourceManager: ResourceManager,
    private val preferences: Preferences
) : ProxySigningPresenter {

    override suspend fun requestResume(proxiedMetaAccount: MetaAccount, proxyMetaAccount: MetaAccount): Boolean = withContext(Dispatchers.Main) {
        if (notNeedToShowWarning(proxiedMetaAccount)) {
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
        proxyTypes: List<ProxyAccount.ProxyType>
    ) = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            ProxySignNotEnoughPermissionBottomSheet(
                context = contextManager.getActivity()!!,
                subtitle = formatNotEnoughPermissionWarning(proxiedMetaAccount, proxyMetaAccount, proxyTypes),
                onSuccess = { continuation.resume(Unit) }
            ).show()
        }
    }

    override suspend fun signingIsNotSupported() = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val bottomSheet = ProxySignOperationNotSupportedBottomSheet(
                contextManager.getActivity()!!,
                onSuccess = { continuation.resume(Unit) }
            )
            bottomSheet.show()
        }
    }

    private fun notNeedToShowWarning(proxyMetaAccount: MetaAccount): Boolean {
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
        proxyTypes: List<ProxyAccount.ProxyType>
    ): CharSequence {
        val subtitle = resourceManager.getString(R.string.proxy_signing_not_enough_permission_message)
        val primaryColor = resourceManager.getColor(R.color.text_primary)

        val proxiedName = proxiedMetaAccount.name.toSpannable(colorSpan(primaryColor))
        val proxyName = proxyMetaAccount.name.toSpannable(colorSpan(primaryColor))

        val proxyTypesBuffer = SpannableStringBuilder()
        val proxyTypesCharSequence = proxyTypes.joinTo(proxyTypesBuffer) { it.name.toSpannable(colorSpan(primaryColor)) }

        return SpannableFormatter.format(subtitle, proxiedName, proxyName, proxyTypesCharSequence)
    }
}

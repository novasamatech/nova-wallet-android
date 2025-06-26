package io.novafoundation.nova.feature_account_impl.presentation.sign

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.requireActivity
import io.novafoundation.nova.common.utils.LazyGet
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface NestedSigningPresenter {

    suspend fun acknowledgeNestedSignOperation(
        warningShowFor: MetaAccount,
        title: LazyGet<String>,
        subtitle: LazyGet<CharSequence>,
        iconRes: LazyGet<Int>
    ): Boolean

    suspend fun presentValidationFailure(title: String, message: CharSequence)
}

@FeatureScope
class RealNestedSigningPresenter @Inject constructor(
    private val contextManager: ContextManager,
    private val preferences: Preferences,
) : NestedSigningPresenter {

    companion object {

        // We intentionally leave key to be the same as it was before in proxies
        // so previous choices wont be reset
        private const val KEY_DONT_SHOW_AGAIN = "proxy_sign_warning_dont_show_again"
    }

    override suspend fun acknowledgeNestedSignOperation(
        warningShowFor: MetaAccount,
        title: LazyGet<String>,
        subtitle: LazyGet<CharSequence>,
        iconRes: LazyGet<Int>
    ): Boolean = withContext(Dispatchers.Main) {
        if (noNeedToShowWarning(warningShowFor)) {
            return@withContext true
        }

        val resumingAllowed = suspendCoroutine { continuation ->
            NestedSignWarningBottomSheet(
                context = contextManager.requireActivity(),
                title = title(),
                subtitle = subtitle(),
                iconRes = iconRes(),
                onFinish = { continuation.resume(it) },
                dontShowAgain = { dontShowAgain(warningShowFor) }
            ).show()
        }

        return@withContext resumingAllowed
    }

    override suspend fun presentValidationFailure(title: String, message: CharSequence) = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            dialog(contextManager.getActivity()!!) {
                setTitle(title)
                setMessage(message)

                setPositiveButton(io.novafoundation.nova.common.R.string.common_close) { _, _ -> continuation.resume(Unit) }
            }
        }
    }

    private fun noNeedToShowWarning(metaAccount: MetaAccount): Boolean {
        return preferences.getBoolean(makePrefsKey(metaAccount), false)
    }

    private fun dontShowAgain(metaAccount: MetaAccount) {
        preferences.putBoolean(makePrefsKey(metaAccount), true)
    }

    private fun makePrefsKey(metaAccount: MetaAccount): String {
        return "${KEY_DONT_SHOW_AGAIN}_${metaAccount.id}"
    }
}

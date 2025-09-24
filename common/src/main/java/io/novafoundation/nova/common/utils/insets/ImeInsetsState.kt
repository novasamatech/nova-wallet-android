package io.novafoundation.nova.common.utils.insets

import android.os.Build

enum class ImeInsetsState(val enabled: Boolean) {
    ENABLE(true),
    DISABLE(false),

    /**
     * Android 10 and lower doesn't support ime insets so we may have wrong insets in case when dappEntryPoint is shown
     * So this state is useful to prevent use ime insets only for supported APIs
     * See: [SplitScreenFragment.manageInsets].
     */
    ENABLE_IF_SUPPORTED(isImeInsetsSupported())
}

private fun isImeInsetsSupported() = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q

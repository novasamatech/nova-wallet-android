package io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.mixin

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val SHOW_DIALOG_KEY = "SHOW_DIALOG_KEY"

interface UnifiedAddressMixin {

    val dontShowUnifiedAddressDialogFlow: MutableStateFlow<Boolean>

    fun shouldShowDialog(): Boolean
}

class RealUnifiedAddressMixin(
    private val preferences: Preferences,
    private val coroutineScope: CoroutineScope
) : UnifiedAddressMixin {

    override val dontShowUnifiedAddressDialogFlow = MutableStateFlow(!shouldShowDialog())

    init {
        dontShowUnifiedAddressDialogFlow
            .onEach { setShouldShowDialog(!it) }
            .launchIn(coroutineScope)
    }

    override fun shouldShowDialog(): Boolean {
        return preferences.getBoolean(SHOW_DIALOG_KEY, true)
    }

    private fun setShouldShowDialog(show: Boolean) {
        preferences.putBoolean(SHOW_DIALOG_KEY, show)
    }
}

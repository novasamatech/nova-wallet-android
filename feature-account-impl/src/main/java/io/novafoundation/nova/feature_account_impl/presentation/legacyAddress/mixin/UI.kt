package io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.mixin

import android.widget.CompoundButton
import io.novafoundation.nova.common.utils.bindTo
import kotlinx.coroutines.CoroutineScope

fun UnifiedAddressMixin.bindWith(compoundButton: CompoundButton, coroutineScope: CoroutineScope) {
    compoundButton.bindTo(dontShowUnifiedAddressDialogFlow, coroutineScope)
}

package io.novafoundation.nova.feature_account_api.presenatation.addressActions

import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragmentMixin

context(BaseFragmentMixin<*>)
fun AddressActionsMixin.setupAddressActions() {
    showAddressActionsEvent.observeEvent { payload ->
        lifecycleOwner.lifecycleScope.launchWhenResumed {
            AddressActionsSheet(
                context = providedContext,
                payload = payload,
                onCopy = ::copyValue,
            ).show()
        }
    }
}

package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import androidx.core.view.isVisible
import io.novafoundation.nova.common.view.Switch
import io.novafoundation.nova.feature_staking_impl.R

fun Switch.bindMevProtection(state: BittensorMevSwitchState, editable: Boolean, onChanged: (Boolean) -> Unit) {
    isVisible = state != BittensorMevSwitchState.HIDDEN
    field.setOnCheckedChangeListener(null)
    field.isChecked = state.checked
    field.isEnabled = state.enabled && editable
    field.contentDescription = context.getString(R.string.bittensor_mev_title)
    setSubtitle(context.getString(if (state == BittensorMevSwitchState.UNAVAILABLE) R.string.bittensor_mev_unavailable else R.string.bittensor_mev_description))
    field.setOnCheckedChangeListener { _, enabled -> onChanged(enabled) }
}

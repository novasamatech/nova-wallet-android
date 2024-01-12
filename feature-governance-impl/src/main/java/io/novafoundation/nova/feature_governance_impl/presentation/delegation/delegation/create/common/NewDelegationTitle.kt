package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_impl.R

internal fun ResourceManager.newDelegationTitle(isEditMode: Boolean): String {
    val resId = if (isEditMode) R.string.delegation_edit_delegation else R.string.common_add_delegation

    return getString(resId)
}

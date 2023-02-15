package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list

import android.text.TextUtils
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoterModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateLabelModel

sealed interface VoterRvItem {
    val voter: VoterModel
    val metadata: DelegateLabelModel?
    val addressEllipsize: TextUtils.TruncateAt
}

class ExpandableVoterRVItem(
    val primaryIndex: Int,
    override val voter: VoterModel,
    override val metadata: DelegateLabelModel?,
    val isExpandable: Boolean,
    val isExpanded: Boolean,
    val showConviction: Boolean,
    override val addressEllipsize: TextUtils.TruncateAt
) : VoterRvItem

class DelegatorVoterRVItem(
    override val voter: VoterModel,
    override val metadata: DelegateLabelModel?,
    override val addressEllipsize: TextUtils.TruncateAt
) : VoterRvItem

package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list

import android.text.TextUtils
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateLabelModel

sealed interface VoterRvItem {
    val vote: VoteModel
    val metadata: DelegateLabelModel
    val addressEllipsize: TextUtils.TruncateAt
}

class ExpandableVoterRVItem(
    val primaryIndex: Int,
    override val vote: VoteModel,
    override val metadata: DelegateLabelModel,
    val isExpandable: Boolean,
    val isExpanded: Boolean,
    override val addressEllipsize: TextUtils.TruncateAt
) : VoterRvItem

class DelegatorVoterRVItem(
    override val vote: VoteModel,
    override val metadata: DelegateLabelModel,
    override val addressEllipsize: TextUtils.TruncateAt
) : VoterRvItem

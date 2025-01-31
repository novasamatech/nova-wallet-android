package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda

import android.os.Parcelable
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.parcelize.Parcelize

@Parcelize
class VotedReferendaPayload(
    val accountId: AccountId,
    val onlyRecentVotes: Boolean,
    val overriddenTitle: String?
) : Parcelable

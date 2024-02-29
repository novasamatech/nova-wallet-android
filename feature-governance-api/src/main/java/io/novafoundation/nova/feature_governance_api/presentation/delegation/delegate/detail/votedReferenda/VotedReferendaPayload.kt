package io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.detail.votedReferenda

import android.os.Parcelable
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class VotedReferendaPayload(
    val accountId: AccountId,
    val onlyRecentVotes: Boolean,
    val overriddenTitle: String?
) : Parcelable

package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

@Parcelize
class ConfirmVoteReferendumPayload(
    val _referendumId: BigInteger,
    val fee: BigDecimal,
    val vote: AccountVoteParcelModel
) : Parcelable

val ConfirmVoteReferendumPayload.referendumId: ReferendumId
    get() = ReferendumId(_referendumId)

@Parcelize
class AccountVoteParcelModel(
    val amount: BigDecimal,
    val conviction: Conviction,
    val aye: Boolean
) : Parcelable

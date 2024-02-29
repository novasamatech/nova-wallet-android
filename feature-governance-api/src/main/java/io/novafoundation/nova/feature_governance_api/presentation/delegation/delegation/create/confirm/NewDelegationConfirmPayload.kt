package io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

@Parcelize
class NewDelegationConfirmPayload(
    val delegate: AccountId,
    @Suppress("CanBeParameter") val trackIdsRaw: List<BigInteger>,
    val amount: BigDecimal,
    val conviction: Conviction,
    val fee: FeeParcelModel,
    val isEditMode: Boolean,
) : Parcelable {

    val trackIds = trackIdsRaw.map(::TrackId)
}

val NewDelegationConfirmPayload.convictionVote: GenericVoter.ConvictionVote
    get() {
        return GenericVoter.ConvictionVote(amount, conviction)
    }

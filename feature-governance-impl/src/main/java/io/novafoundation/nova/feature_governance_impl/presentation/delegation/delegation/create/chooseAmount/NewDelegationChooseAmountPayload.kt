package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
class NewDelegationChooseAmountPayload(
    val delegate: AccountId,
    @Suppress("CanBeParameter") // val is required for Parcelize to work
    private val _trackIdsRaw: List<BigInteger>
) : Parcelable {

    val trackIds = _trackIdsRaw.map(::TrackId)
}

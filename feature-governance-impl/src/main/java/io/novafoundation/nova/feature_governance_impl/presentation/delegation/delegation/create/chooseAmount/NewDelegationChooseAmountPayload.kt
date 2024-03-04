package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
class NewDelegationChooseAmountPayload(
    val delegate: AccountId,
    @Suppress("CanBeParameter") // val is required for Parcelize to work
    val trackIdsRaw: List<BigInteger>,
    val isEditMode: Boolean,
) : Parcelable {

    val trackIds = trackIdsRaw.map(::TrackId)
}

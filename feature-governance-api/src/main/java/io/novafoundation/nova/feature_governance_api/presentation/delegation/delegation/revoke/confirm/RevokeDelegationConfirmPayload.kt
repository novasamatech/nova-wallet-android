package io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.revoke.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
class RevokeDelegationConfirmPayload(
    val delegateId: AccountId,
    @Suppress("CanBeParameter") val trackIdsRaw: List<BigInteger>,
) : Parcelable {

    val trackIds = trackIdsRaw.map(::TrackId)
}

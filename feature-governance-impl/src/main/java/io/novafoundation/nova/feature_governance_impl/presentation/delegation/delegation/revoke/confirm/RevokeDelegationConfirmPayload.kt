package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
class RevokeDelegationConfirmPayload(
    val delegateId: AccountId,
    @Suppress("CanBeParameter") val trackIdsRaw: List<BigInteger>,
) : Parcelable {

    val trackIds = trackIdsRaw.map(::TrackId)
}

package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import kotlinx.android.parcel.Parcelize

@Parcelize
class ContributePayload(
    val paraId: ParaId,
    val parachainMetadata: ParachainMetadataParcelModel?
) : Parcelable

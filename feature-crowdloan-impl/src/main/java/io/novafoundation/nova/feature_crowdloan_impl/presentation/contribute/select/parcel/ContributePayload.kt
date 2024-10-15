package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel

import android.os.Parcelable
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import kotlinx.parcelize.Parcelize

@Parcelize
class ContributePayload(
    val paraId: ParaId,
    val parachainMetadata: ParachainMetadataParcelModel?
) : Parcelable

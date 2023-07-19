package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

@Parcelize
class ParachainMetadataParcelModel(
    val paraId: BigInteger,
    val movedToParaId: BigInteger?,
    val iconLink: String,
    val name: String,
    val description: String,
    val rewardRate: BigDecimal?,
    val website: String,
    val customFlow: String?,
    val token: String,
    val extras: Map<String, String>,
) : Parcelable

fun mapParachainMetadataToParcel(
    parachainMetadata: ParachainMetadata,
) = with(parachainMetadata) {
    ParachainMetadataParcelModel(
        paraId = paraId,
        movedToParaId = movedToParaId,
        iconLink = iconLink,
        name = name,
        description = description,
        rewardRate = rewardRate,
        website = website,
        token = token,
        customFlow = customFlow,
        extras = parachainMetadata.extras
    )
}

fun mapParachainMetadataFromParcel(
    parcelModel: ParachainMetadataParcelModel,
) = with(parcelModel) {
    ParachainMetadata(
        paraId = paraId,
        movedToParaId = movedToParaId,
        iconLink = iconLink,
        name = name,
        description = description,
        rewardRate = rewardRate,
        customFlow = customFlow,
        website = website,
        token = token,
        extras = extras
    )
}

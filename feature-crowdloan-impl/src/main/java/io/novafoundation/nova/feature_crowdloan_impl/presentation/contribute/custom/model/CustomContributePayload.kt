package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model

import android.os.Parcelable
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ParachainMetadataParcelModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
class CustomContributePayload(
    val paraId: ParaId,
    val parachainMetadata: ParachainMetadataParcelModel,
    val amount: BigDecimal,
    val previousBonusPayload: BonusPayload?
) : Parcelable

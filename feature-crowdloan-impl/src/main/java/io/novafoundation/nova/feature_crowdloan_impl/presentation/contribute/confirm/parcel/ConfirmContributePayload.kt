package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel

import android.os.Parcelable
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ParachainMetadataParcelModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmContributePayload(
    val paraId: ParaId,
    val fee: FeeParcelModel,
    val amount: BigDecimal,
    val bonusPayload: BonusPayload?,
    val customizationPayload: Parcelable?,
    val metadata: ParachainMetadataParcelModel?,
    val estimatedRewardDisplay: String?,
) : Parcelable

package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala.ContributionType
import kotlinx.parcelize.Parcelize

@Parcelize
class AcalaCustomizationPayload(
    val contributionType: ContributionType,
) : Parcelable

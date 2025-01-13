package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Response
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.CollatorRecommendationConfigParcelModel
import kotlinx.parcelize.Parcelize

interface SelectCollatorSettingsInterScreenRequester : InterScreenRequester<Request, Response>
interface SelectCollatorSettingsInterScreenResponder : InterScreenResponder<Request, Response>

interface SelectCollatorSettingsInterScreenCommunicator : SelectCollatorSettingsInterScreenRequester, SelectCollatorSettingsInterScreenResponder {

    @Parcelize
    class Response(val newConfig: CollatorRecommendationConfigParcelModel) : Parcelable

    @Parcelize
    class Request(val currentConfig: CollatorRecommendationConfigParcelModel) : Parcelable
}

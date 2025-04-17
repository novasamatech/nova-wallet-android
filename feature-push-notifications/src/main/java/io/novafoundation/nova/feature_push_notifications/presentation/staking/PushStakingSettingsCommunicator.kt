package io.novafoundation.nova.feature_push_notifications.presentation.staking

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

interface PushStakingSettingsRequester : InterScreenRequester<PushStakingSettingsRequester.Request, PushStakingSettingsResponder.Response> {

    @Parcelize
    class Request(val settings: PushStakingSettingsPayload) : Parcelable
}

interface PushStakingSettingsResponder : InterScreenResponder<PushStakingSettingsRequester.Request, PushStakingSettingsResponder.Response> {

    @Parcelize
    class Response(val settings: PushStakingSettingsPayload) : Parcelable
}

interface PushStakingSettingsCommunicator : PushStakingSettingsRequester, PushStakingSettingsResponder

sealed class PushStakingSettingsPayload : Parcelable {

    @Parcelize
    object AllChains : PushStakingSettingsPayload()

    @Parcelize
    class SpecifiedChains(val enabledChainIds: Set<ChainId>) : PushStakingSettingsPayload()
}

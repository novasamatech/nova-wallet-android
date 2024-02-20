package io.novafoundation.nova.feature_push_notifications.data.presentation.governance

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

interface PushGovernanceSettingsRequester : InterScreenRequester<PushGovernanceSettingsRequester.Request, PushGovernanceSettingsResponder.Response> {

    @Parcelize
    class Request(val enabledGovernanceSettings: List<PushGovernanceSettings>) : Parcelable
}

interface PushGovernanceSettingsResponder : InterScreenResponder<PushGovernanceSettingsRequester.Request, PushGovernanceSettingsResponder.Response> {

    @Parcelize
    class Response(val enabledGovernanceSettings: List<PushGovernanceSettings>) : Parcelable
}

interface PushGovernanceSettingsCommunicator : PushGovernanceSettingsRequester, PushGovernanceSettingsResponder

@Parcelize
class PushGovernanceSettings(
    val chainId: ChainId,
    val governance: Chain.Governance,
    val newReferenda: Boolean,
    val referendaUpdates: Boolean,
    val delegateVotes: Boolean,
    val tracks: Tracks
) : Parcelable {

    sealed interface Tracks : Parcelable {

        @Parcelize
        object All : Tracks

        @Parcelize
        data class Specified(val items: List<String>) : Tracks
    }
}

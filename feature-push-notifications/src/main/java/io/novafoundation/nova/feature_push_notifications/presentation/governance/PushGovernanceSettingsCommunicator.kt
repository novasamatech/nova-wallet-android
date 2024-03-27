package io.novafoundation.nova.feature_push_notifications.presentation.governance

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

interface PushGovernanceSettingsRequester : InterScreenRequester<PushGovernanceSettingsRequester.Request, PushGovernanceSettingsResponder.Response> {

    @Parcelize
    class Request(val enabledGovernanceSettings: List<PushGovernanceSettingsPayload>) : Parcelable
}

interface PushGovernanceSettingsResponder : InterScreenResponder<PushGovernanceSettingsRequester.Request, PushGovernanceSettingsResponder.Response> {

    @Parcelize
    class Response(val enabledGovernanceSettings: List<PushGovernanceSettingsPayload>) : Parcelable
}

interface PushGovernanceSettingsCommunicator : PushGovernanceSettingsRequester, PushGovernanceSettingsResponder

@Parcelize
class PushGovernanceSettingsPayload(
    val chainId: ChainId,
    val governance: Chain.Governance,
    val newReferenda: Boolean,
    val referendaUpdates: Boolean,
    val delegateVotes: Boolean,
    val tracksIds: Set<BigInteger>
) : Parcelable

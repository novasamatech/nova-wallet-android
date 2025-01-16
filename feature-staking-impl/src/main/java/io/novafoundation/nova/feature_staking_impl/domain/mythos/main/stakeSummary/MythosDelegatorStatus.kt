package io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary

sealed class MythosDelegatorStatus {

    object Active : MythosDelegatorStatus()

    object Inactive : MythosDelegatorStatus()
}

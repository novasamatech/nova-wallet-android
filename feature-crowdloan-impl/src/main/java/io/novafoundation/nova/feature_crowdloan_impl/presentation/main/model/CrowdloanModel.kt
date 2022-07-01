package io.novafoundation.nova.feature_crowdloan_impl.presentation.main.model

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class CrowdloanStatusModel(
    val status: String,
    val count: String,
)

data class CrowdloanModel(
    val relaychainId: ChainId,
    val parachainId: ParaId,
    val title: String,
    val description: String,
    val icon: Icon,
    val raised: Raised,
    val state: State,
) {

    data class Raised(
        val value: String,
        val percentage: Int, // 0..100
        val percentageDisplay: String,
    )

    sealed class State {
        object Finished : State()

        data class Active(val timeRemaining: String) : State()
    }
}

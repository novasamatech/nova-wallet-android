package io.novafoundation.nova.app.root.presentation.main

import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel(
    interactor: RootInteractor,
    externalRequirements: MutableStateFlow<ChainConnection.ExternalRequirement>,
) : BaseViewModel() {

    init {
        externalRequirements.value = ChainConnection.ExternalRequirement.ALLOWED
    }

    val stakingAvailableLiveData = interactor.stakingAvailableFlow()
        .asLiveData()
}

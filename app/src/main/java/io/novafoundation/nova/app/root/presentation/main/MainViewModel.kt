package io.novafoundation.nova.app.root.presentation.main

import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.base.BaseViewModel

class MainViewModel(
    interactor: RootInteractor,
) : BaseViewModel() {

    val stakingAvailableLiveData = interactor.stakingAvailableFlow()
        .asLiveData()
}

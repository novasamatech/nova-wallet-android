package io.novafoundation.nova.app.root.presentation.main

import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

class MainViewModel(
    interactor: RootInteractor,
    updateNotificationsInteractor: UpdateNotificationsInteractor
) : BaseViewModel() {

    val stakingAvailableLiveData = interactor.stakingAvailableFlow()
        .asLiveData()

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()
    }
}

package io.novafoundation.nova.app.root.presentation.main

import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

class MainViewModel(
    interactor: RootInteractor,
    updateNotificationsInteractor: UpdateNotificationsInteractor,
    private val automaticInteractionGate: AutomaticInteractionGate,
) : BaseViewModel() {

    val stakingAvailableLiveData = interactor.stakingAvailableFlow()
        .asLiveData()

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()
        automaticInteractionGate.initialPinPassed()
    }
}

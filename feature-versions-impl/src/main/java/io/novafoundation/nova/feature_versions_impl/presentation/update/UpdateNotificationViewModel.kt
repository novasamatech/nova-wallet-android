package io.novafoundation.nova.feature_versions_impl.presentation.update

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter

class UpdateNotificationViewModel(
    private val router: VersionsRouter,
    private val interactor: UpdateNotificationsInteractor
) : BaseViewModel() {

}

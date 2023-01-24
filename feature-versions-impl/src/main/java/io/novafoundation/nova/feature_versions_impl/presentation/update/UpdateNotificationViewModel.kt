package io.novafoundation.nova.feature_versions_impl.presentation.update

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.formatDateSinceEpoch
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_versions_api.domain.Severity
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter
import io.novafoundation.nova.feature_versions_impl.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class UpdateNotificationViewModel(
    private val router: VersionsRouter,
    private val interactor: UpdateNotificationsInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val showAllNotifications = MutableStateFlow(false)

    private val notifications = flowOf { interactor.getUpdateNotifications() }

    val notificationModels = combine(showAllNotifications, notifications) { shouldShowAll, notifications ->
        val result = if (shouldShowAll) {
            notifications
        } else {
            notifications.take(1)
        }
        buildList {
            val banner = getBannerOrNull(notifications)
            banner?.let { add(it) }
            addAll(mapUpdateNotificationsToModels(result))
            if (notifications.size > 1 && !shouldShowAll) {
                add(SeeAllButtonModel())
            }
        }
    }.withLoading()

    fun skipClicked() {
        launch {
            interactor.skipNewUpdates()
            router.back()
        }
    }

    fun installUpdateClicked() {
        router.openInstallUpdates()
    }

    fun showAllNotifications() {
        showAllNotifications.value = true
    }

    private fun hasCriticalUpdates(list: List<UpdateNotification>): Boolean {
        return list.any {
            it.severity == Severity.CRITICAL
        }
    }

    private fun hasMajorUpdates(list: List<UpdateNotification>): Boolean {
        return list.any {
            it.severity == Severity.MAJOR
        }
    }

    private fun mapUpdateNotificationsToModels(list: List<UpdateNotification>): List<UpdateNotificationModel> {
        return list.mapIndexed { index, it ->
            UpdateNotificationModel(
                version = it.version.toString(),
                changelog = it.changelog,
                isLastUpdate = index == 0,
                severity = mapSeverity(it.severity),
                severityColorRes = mapSeverityColor(it.severity),
                severityBackgroundRes = mapSeverityBackground(it.severity),
                date = it.time.formatDateSinceEpoch(resourceManager)
            )
        }
    }

    private fun mapSeverity(severity: Severity): String? {
        return when (severity) {
            Severity.CRITICAL -> resourceManager.getString(R.string.update_notifications_severity_critical)
            Severity.MAJOR -> resourceManager.getString(R.string.update_notifications_severity_major)
            Severity.NORMAL -> null
        }
    }

    private fun mapSeverityColor(severity: Severity): Int? {
        return when (severity) {
            Severity.CRITICAL -> R.color.text_warning
            Severity.MAJOR -> R.color.chip_text
            Severity.NORMAL -> null
        }
    }

    private fun mapSeverityBackground(severity: Severity): Int? {
        return when (severity) {
            Severity.CRITICAL -> R.color.warning_block_background
            Severity.MAJOR -> R.color.chips_background
            Severity.NORMAL -> null
        }
    }

    private fun getBannerOrNull(notifications: List<UpdateNotification>): UpdateNotificationBannerModel? {
        if (hasCriticalUpdates(notifications)) {
            return UpdateNotificationBannerModel(
                R.drawable.ic_critical_update,
                R.drawable.ic_banner_yellow_gradient,
                resourceManager.getString(R.string.update_notifications_critical_update_alert_titile),
                resourceManager.getString(R.string.update_notifications_critical_update_alert_subtitile)
            )
        } else if (hasMajorUpdates(notifications)) {
            return UpdateNotificationBannerModel(
                R.drawable.ic_major_update,
                R.drawable.ic_banner_turquoise_gradient,
                resourceManager.getString(R.string.update_notifications_major_update_alert_titile),
                resourceManager.getString(R.string.update_notifications_major_update_alert_subtitile)
            )
        }

        return null
    }
}

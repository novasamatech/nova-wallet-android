package io.novafoundation.nova.feature_versions_impl.presentation.update

import io.noties.markwon.Markwon
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
import io.novafoundation.nova.feature_versions_impl.presentation.update.models.UpdateNotificationBannerModel
import io.novafoundation.nova.feature_versions_impl.presentation.update.models.UpdateNotificationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class UpdateNotificationViewModel(
    private val router: VersionsRouter,
    private val interactor: UpdateNotificationsInteractor,
    private val resourceManager: ResourceManager,
    private val markwon: Markwon,
) : BaseViewModel() {

    private val showAllNotifications = MutableStateFlow(false)

    private val notifications = flowOf { interactor.getUpdateNotifications() }

    val bannerModel = notifications.map { getBannerOrNull(it) }
        .shareInBackground()

    val notificationModels = combine(showAllNotifications, notifications) { shouldShowAll, notifications ->
        val result = if (shouldShowAll) {
            notifications
        } else {
            notifications.take(1)
        }
        mapUpdateNotificationsToModels(result)
    }
        .withLoading()
        .shareInBackground()

    val seeAllButtonVisible = combine(showAllNotifications, notifications) { shouldShowAll, notifications ->
        notifications.size > 1 && !shouldShowAll
    }
        .shareInBackground()

    fun skipClicked() = launch {
        interactor.skipNewUpdates()
        router.closeUpdateNotifications()
    }

    fun installUpdateClicked() {
        router.closeUpdateNotifications()
        router.openAppUpdater()
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
        return list.mapIndexed { index, version ->
            UpdateNotificationModel(
                version = version.version.toString(),
                changelog = version.changelog?.let { markwon.toMarkdown(it) },
                isLatestUpdate = index == 0,
                severity = mapSeverity(version.severity),
                severityColorRes = mapSeverityColor(version.severity),
                severityBackgroundRes = mapSeverityBackground(version.severity),
                date = version.time.formatDateSinceEpoch(resourceManager)
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
            Severity.CRITICAL -> R.color.critical_update_chip_text
            Severity.MAJOR -> R.color.major_update_chip_text
            Severity.NORMAL -> null
        }
    }

    private fun mapSeverityBackground(severity: Severity): Int? {
        return when (severity) {
            Severity.CRITICAL -> R.color.critical_update_chip_background
            Severity.MAJOR -> R.color.major_update_chip_background
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

package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings

import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings.views.CloudBackupStateView

class CloudBackupStateModel(
    val stateImg: Int?,
    val stateImageTint: Int,
    val stateColorBackgroundRes: Int?,
    val showProgress: Boolean,
    val title: String,
    val subtitle: String?,
    val isClickable: Boolean,
    val problemButtonText: String?
)

fun CloudBackupStateView.setState(state: CloudBackupStateModel) {
    setStateImage(state.stateImg, state.stateImageTint, state.stateColorBackgroundRes)
    setProgressVisibility(state.showProgress)
    setTitle(state.title)
    setSubtitle(state.subtitle)
    isClickable = state.isClickable
    showMoreButton(state.isClickable)
    setProblemText(state.problemButtonText)
}

package io.novafoundation.nova.feature_versions_impl.presentation.update

class UpdateNotificationAlertModel(
    val message: String
)

class UpdateNotificationModel(
    val updateType: String,
    val versionTitle: String,
    val versionDescription: String,
    val date: String
)

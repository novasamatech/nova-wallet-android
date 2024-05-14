package io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin

import io.novafoundation.nova.common.base.BaseScreenMixin
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet

fun BaseScreenMixin<*>.observeConfirmationAction(mixinFactory: CloudBackupChangingWarningMixin) {
    observeActionBottomSheet(mixinFactory.actionBottomSheetLauncher)
}

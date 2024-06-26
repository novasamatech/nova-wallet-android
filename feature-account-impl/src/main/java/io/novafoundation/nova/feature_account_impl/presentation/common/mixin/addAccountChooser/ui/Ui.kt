package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.ui

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.setupImportTypeChooser
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.observeConfirmationAction

fun BaseFragment<*>.setupAddAccountLauncher(mixin: AddAccountLauncherMixin) {
    val asImportTypeChooser = object : ImportTypeChooserMixin {
        override val showChooserEvent = mixin.showImportTypeChooser
    }
    setupImportTypeChooser(asImportTypeChooser)
    observeConfirmationAction(mixin.cloudBackupChangingWarningMixin)

    mixin.showAddAccountTypeChooser.observeEvent {
        AddAccountChooserBottomSheet(
            context = requireContext(),
            payload = it
        ).show()
    }
}

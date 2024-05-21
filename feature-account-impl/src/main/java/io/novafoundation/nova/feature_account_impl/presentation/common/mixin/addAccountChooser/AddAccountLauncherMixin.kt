package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixin
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

interface AddAccountLauncherPresentationFactory {

    fun create(
        scope: CoroutineScope
    ): AddAccountLauncherMixin.Presentation
}

interface AddAccountLauncherMixin {

    class AddAccountTypePayload(
        val title: String,
        val onCreate: () -> Unit,
        val onImport: () -> Unit
    )

    val cloudBackupChangingWarningMixin: CloudBackupChangingWarningMixin

    val showAddAccountTypeChooser: LiveData<Event<AddAccountTypePayload>>

    val showImportTypeChooser: LiveData<Event<ImportTypeChooserMixin.Payload>>

    interface Presentation : AddAccountLauncherMixin {

        fun initiateLaunch(
            chain: Chain,
            metaAccount: MetaAccount,
        )
    }
}

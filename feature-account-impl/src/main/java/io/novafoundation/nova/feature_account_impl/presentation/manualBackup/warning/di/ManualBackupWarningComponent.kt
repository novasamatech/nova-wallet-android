package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning.ManualBackupWarningFragment

@Subcomponent(
    modules = [
        ManualBackupWarningModule::class
    ]
)
@ScreenScope
interface ManualBackupWarningComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ManualBackupCommonPayload
        ): ManualBackupWarningComponent
    }

    fun inject(fragment: ManualBackupWarningFragment)
}

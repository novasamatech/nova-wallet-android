package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.ManualBackupSecretsFragment
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning.ManualBackupWarningFragment

@Subcomponent(
    modules = [
        ManualBackupSecretsModule::class
    ]
)
@ScreenScope
interface ManualBackupSecretsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ManualBackupCommonPayload
        ): ManualBackupSecretsComponent
    }

    fun inject(fragment: ManualBackupSecretsFragment)
}

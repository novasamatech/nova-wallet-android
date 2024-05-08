package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.advanced.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.advanced.ManualBackupAdvancedSecretsFragment
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload

@Subcomponent(
    modules = [
        ManualBackupAdvancedSecretsModule::class
    ]
)
@ScreenScope
interface ManualBackupAdvancedSecretsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ManualBackupCommonPayload
        ): ManualBackupAdvancedSecretsComponent
    }

    fun inject(fragment: ManualBackupAdvancedSecretsFragment)
}

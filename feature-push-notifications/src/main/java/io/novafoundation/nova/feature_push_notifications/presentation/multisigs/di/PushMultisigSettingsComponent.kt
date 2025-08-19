package io.novafoundation.nova.feature_push_notifications.presentation.multisigs.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsFragment
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsRequester
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsFragment
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsRequester
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsViewModel

@Subcomponent(
    modules = [
        PushMultisigSettingsModule::class
    ]
)
@ScreenScope
interface PushMultisigSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: PushMultisigSettingsRequester.Request
        ): PushMultisigSettingsComponent
    }

    fun inject(fragment: PushMultisigSettingsFragment)
}

package io.novafoundation.nova.feature_push_notifications.presentation.governance.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsFragment
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsRequester

@Subcomponent(
    modules = [
        PushGovernanceSettingsModule::class
    ]
)
@ScreenScope
interface PushGovernanceSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: PushGovernanceSettingsRequester.Request
        ): PushGovernanceSettingsComponent
    }

    fun inject(fragment: PushGovernanceSettingsFragment)
}

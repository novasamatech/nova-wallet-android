package io.novafoundation.nova.feature_push_notifications.data.presentation.staking.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_push_notifications.data.presentation.staking.PushStakingSettingsFragment
import io.novafoundation.nova.feature_push_notifications.data.presentation.staking.PushStakingSettingsRequester

@Subcomponent(
    modules = [
        PushStakingSettingsModule::class
    ]
)
@ScreenScope
interface PushStakingSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: PushStakingSettingsRequester.Request
        ): PushStakingSettingsComponent
    }

    fun inject(fragment: PushStakingSettingsFragment)
}

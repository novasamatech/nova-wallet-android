package io.novafoundation.nova.feature_push_notifications.data.presentation.welcome.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_push_notifications.data.presentation.welcome.PushWelcomeFragment

@Subcomponent(
    modules = [
        PushWelcomeModule::class
    ]
)
@ScreenScope
interface PushWelcomeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): PushWelcomeComponent
    }

    fun inject(fragment: PushWelcomeFragment)
}

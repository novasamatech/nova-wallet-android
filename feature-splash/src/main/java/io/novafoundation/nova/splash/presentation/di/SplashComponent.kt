package io.novafoundation.nova.splash.presentation.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.splash.presentation.SplashFragment

@Subcomponent(
    modules = [
        SplashModule::class
    ]
)
@ScreenScope
interface SplashComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): SplashComponent
    }

    fun inject(fragment: SplashFragment)
}

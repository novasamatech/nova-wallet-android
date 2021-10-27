package io.novafoundation.nova.feature_account_impl.presentation.about.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.about.AboutFragment

@Subcomponent(
    modules = [
        AboutModule::class
    ]
)
@ScreenScope
interface AboutComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): AboutComponent
    }

    fun inject(aboutFragment: AboutFragment)
}

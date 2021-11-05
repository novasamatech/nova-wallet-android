package io.novafoundation.nova.feature_account_impl.presentation.profile.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.profile.ProfileFragment

@Subcomponent(
    modules = [
        ProfileModule::class
    ]
)
@ScreenScope
interface ProfileComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): ProfileComponent
    }

    fun inject(profileFragment: ProfileFragment)
}

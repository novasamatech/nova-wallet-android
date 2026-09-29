package io.novafoundation.nova.feature_settings_impl.presentation.privacy.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.privacy.PrivacyFragment

@Subcomponent(
    modules = [
        PrivacyModule::class
    ]
)
@ScreenScope
interface PrivacyComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): PrivacyComponent
    }

    fun inject(fragment: PrivacyFragment)
}

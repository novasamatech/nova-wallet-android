package io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser.ImportWalletOptionsFragment

@Subcomponent(
    modules = [
        ImportWalletOptionsModule::class
    ]
)
@ScreenScope
interface ImportWalletOptionsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ImportWalletOptionsComponent
    }

    fun inject(fragment: ImportWalletOptionsFragment)
}

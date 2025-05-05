package io.novafoundation.nova.feature_pay_impl.presentation.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_pay_impl.presentation.main.PayMainFragment

@Subcomponent(
    modules = [
        PayMainModule::class
    ]
)
@ScreenScope
interface PayMainComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): PayMainComponent
    }

    fun inject(welcomeFragment: PayMainFragment)
}

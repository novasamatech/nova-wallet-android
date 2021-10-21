package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom.CustomRebondFragment

@Subcomponent(
    modules = [
        CustomRebondModule::class
    ]
)
@ScreenScope
interface CustomRebondComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): CustomRebondComponent
    }

    fun inject(fragment: CustomRebondFragment)
}

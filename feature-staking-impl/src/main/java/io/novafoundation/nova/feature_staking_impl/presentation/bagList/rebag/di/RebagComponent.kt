package io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag.RebagFragment

@Subcomponent(
    modules = [
        RebagModule::class
    ]
)
@ScreenScope
interface RebagComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): RebagComponent
    }

    fun inject(fragment: RebagFragment)
}

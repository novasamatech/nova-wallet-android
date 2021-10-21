package io.novafoundation.nova.feature_crowdloan_impl.presentation.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.CrowdloanFragment

@Subcomponent(
    modules = [
        CrowdloanModule::class
    ]
)
@ScreenScope
interface CrowdloanComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): CrowdloanComponent
    }

    fun inject(fragment: CrowdloanFragment)
}

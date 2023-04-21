package io.novafoundation.nova.feature_governance_impl.presentation.referenda.filters.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.filters.ReferendaFiltersFragment

@Subcomponent(
    modules = [
        ReferendaFiltersModule::class
    ]
)
@ScreenScope
interface ReferendaFiltersComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ReferendaFiltersComponent
    }

    fun inject(fragment: ReferendaFiltersFragment)
}

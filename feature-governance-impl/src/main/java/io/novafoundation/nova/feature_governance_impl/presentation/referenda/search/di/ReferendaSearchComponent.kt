package io.novafoundation.nova.feature_governance_impl.presentation.referenda.search.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.search.ReferendaSearchFragment

@Subcomponent(
    modules = [
        ReferendaSearchModule::class
    ]
)
@ScreenScope
interface ReferendaSearchComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ReferendaSearchComponent
    }

    fun inject(fragment: ReferendaSearchFragment)
}

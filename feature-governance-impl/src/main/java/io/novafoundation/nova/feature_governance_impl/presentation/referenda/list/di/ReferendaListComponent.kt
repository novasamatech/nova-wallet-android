package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListFragment

@Subcomponent(
    modules = [
        ReferendaListModule::class
    ]
)
@ScreenScope
interface ReferendaListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ReferendaListComponent
    }

    fun inject(fragment: ReferendaListFragment)
}

package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search.DelegateSearchFragment

@Subcomponent(
    modules = [
        DelegateSearchModule::class
    ]
)
@ScreenScope
interface DelegateSearchComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): DelegateSearchComponent
    }

    fun inject(fragment: DelegateSearchFragment)
}

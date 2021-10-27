package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.search.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.search.SearchCustomValidatorsFragment

@Subcomponent(
    modules = [
        SearchCustomValidatorsModule::class
    ]
)
@ScreenScope
interface SearchCustomValidatorsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SearchCustomValidatorsComponent
    }

    fun inject(fragment: SearchCustomValidatorsFragment)
}

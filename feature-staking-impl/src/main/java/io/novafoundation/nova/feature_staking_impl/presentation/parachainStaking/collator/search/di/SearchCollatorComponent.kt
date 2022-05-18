package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.search.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.search.SearchCollatorFragment

@Subcomponent(
    modules = [
        SearchCollatorValidatorsModule::class
    ]
)
@ScreenScope
interface SearchCollatorComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SearchCollatorComponent
    }

    fun inject(fragment: SearchCollatorFragment)
}

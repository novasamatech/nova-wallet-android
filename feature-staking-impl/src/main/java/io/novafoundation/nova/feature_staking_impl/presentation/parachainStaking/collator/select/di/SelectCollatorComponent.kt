package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.SelectCollatorFragment

@Subcomponent(
    modules = [
        SelectCollatorModule::class
    ]
)
@ScreenScope
interface SelectCollatorComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SelectCollatorComponent
    }

    fun inject(fragment: SelectCollatorFragment)
}

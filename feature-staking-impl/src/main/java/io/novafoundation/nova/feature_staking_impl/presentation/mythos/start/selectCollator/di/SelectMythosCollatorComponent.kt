package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.SelectMythosCollatorFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.SelectCollatorFragment

@Subcomponent(
    modules = [
        SelectMythosCollatorModule::class
    ]
)
@ScreenScope
interface SelectMythosCollatorComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SelectMythosCollatorComponent
    }

    fun inject(fragment: SelectMythosCollatorFragment)
}

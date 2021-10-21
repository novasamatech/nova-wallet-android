package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMorePayload

@Subcomponent(
    modules = [
        SelectBondMoreModule::class
    ]
)
@ScreenScope
interface SelectBondMoreComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectBondMorePayload
        ): SelectBondMoreComponent
    }

    fun inject(fragment: SelectBondMoreFragment)
}

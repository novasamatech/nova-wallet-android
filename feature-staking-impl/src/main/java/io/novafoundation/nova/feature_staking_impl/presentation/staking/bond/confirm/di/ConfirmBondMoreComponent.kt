package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMorePayload

@Subcomponent(
    modules = [
        ConfirmBondMoreModule::class
    ]
)
@ScreenScope
interface ConfirmBondMoreComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmBondMorePayload,
        ): ConfirmBondMoreComponent
    }

    fun inject(fragment: ConfirmBondMoreFragment)
}

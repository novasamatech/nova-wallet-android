package io.novafoundation.nova.feature_staking_impl.presentation.mythos.redeem.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.redeem.MythosRedeemFragment

@Subcomponent(
    modules = [
        MythosRedeemModule::class
    ]
)
@ScreenScope
interface MythosRedeemComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): MythosRedeemComponent
    }

    fun inject(fragment: MythosRedeemFragment)
}

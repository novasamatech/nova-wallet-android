package io.novafoundation.nova.feature_wallet_impl.presentation.balance.detail.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.detail.BalanceDetailFragment

@Subcomponent(
    modules = [
        BalanceDetailModule::class
    ]
)
@ScreenScope
interface BalanceDetailComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance assetPayload: AssetPayload,
        ): BalanceDetailComponent
    }

    fun inject(fragment: BalanceDetailFragment)
}

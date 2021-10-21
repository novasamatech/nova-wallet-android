package io.novafoundation.nova.feature_wallet_impl.presentation.send.amount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.send.amount.ChooseAmountFragment

@Subcomponent(
    modules = [
        ChooseAmountModule::class
    ]
)
@ScreenScope
interface ChooseAmountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance address: String,
            @BindsInstance payload: AssetPayload
        ): ChooseAmountComponent
    }

    fun inject(fragment: ChooseAmountFragment)
}

package io.novafoundation.nova.feature_gift_impl.presentation.amount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_gift_impl.presentation.amount.SelectGiftAmountFragment
import io.novafoundation.nova.feature_gift_impl.presentation.amount.SelectGiftAmountPayload

@Subcomponent(
    modules = [
        SelectGiftAmountModule::class
    ]
)
@ScreenScope
interface SelectGiftAmountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectGiftAmountPayload
        ): SelectGiftAmountComponent
    }

    fun inject(fragment: SelectGiftAmountFragment)
}

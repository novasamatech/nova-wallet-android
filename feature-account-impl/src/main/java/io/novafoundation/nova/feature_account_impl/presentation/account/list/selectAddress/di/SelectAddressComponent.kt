package io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressRequester
import io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress.SelectAddressBottomSheet

@Subcomponent(
    modules = [
        SelectAddressModule::class
    ]
)
@ScreenScope
interface SelectAddressComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: SelectAddressRequester.Request
        ): SelectAddressComponent
    }

    fun inject(fragment: SelectAddressBottomSheet)
}

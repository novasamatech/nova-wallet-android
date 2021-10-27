package io.novafoundation.nova.feature_account_impl.presentation.pincode.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment

@Subcomponent(
    modules = [
        PinCodeModule::class
    ]
)
@ScreenScope
interface PinCodeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance pinCodeAction: PinCodeAction
        ): PinCodeComponent
    }

    fun inject(pincodeFragment: PincodeFragment)
}

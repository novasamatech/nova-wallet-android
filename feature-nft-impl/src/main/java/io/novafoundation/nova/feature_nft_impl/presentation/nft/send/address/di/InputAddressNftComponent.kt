package io.novafoundation.nova.feature_assets.presentation.send.amount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.send.amount.InputAddressNftFragment
import io.novafoundation.nova.feature_nft_impl.presentation.NftPayload

@Subcomponent(
    modules = [
        InputAddressNftModule::class
    ]
)
@ScreenScope
interface InputAddressNftComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: NftPayload
        ): InputAddressNftComponent
    }

    fun inject(fragment: InputAddressNftFragment)
}

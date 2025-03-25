package io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.ChainAddressSelectorFragment
import io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.ChainAddressSelectorPayload

@Subcomponent(
    modules = [
        ChainAddressSelectorModule::class
    ]
)
@ScreenScope
interface ChainAddressSelectorComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ChainAddressSelectorPayload
        ): ChainAddressSelectorComponent
    }

    fun inject(fragment: ChainAddressSelectorFragment)
}

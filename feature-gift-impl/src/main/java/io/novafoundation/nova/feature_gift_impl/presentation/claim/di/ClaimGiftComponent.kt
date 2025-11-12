package io.novafoundation.nova.feature_gift_impl.presentation.claim.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftFragment
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftPayload

@Subcomponent(
    modules = [
        ClaimGiftModule::class
    ]
)
@ScreenScope
interface ClaimGiftComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ClaimGiftPayload
        ): ClaimGiftComponent
    }

    fun inject(fragment: ClaimGiftFragment)
}

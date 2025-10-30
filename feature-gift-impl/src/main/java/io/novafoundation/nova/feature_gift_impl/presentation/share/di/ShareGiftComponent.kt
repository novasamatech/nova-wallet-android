package io.novafoundation.nova.feature_gift_impl.presentation.share.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftFragment
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftPayload

@Subcomponent(
    modules = [
        ShareGiftModule::class
    ]
)
@ScreenScope
interface ShareGiftComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ShareGiftPayload
        ): ShareGiftComponent
    }

    fun inject(fragment: ShareGiftFragment)
}

package io.novafoundation.nova.feature_gift_impl.presentation.gifts.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_gift_impl.presentation.gifts.GiftsFragment

@Subcomponent(
    modules = [
        GiftsModule::class
    ]
)
@ScreenScope
interface GiftsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): GiftsComponent
    }

    fun inject(fragment: GiftsFragment)
}

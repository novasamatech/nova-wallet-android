package io.novafoundation.nova.feature_gift_impl.presentation.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_gift_impl.presentation.confirm.CreateGiftConfirmFragment
import io.novafoundation.nova.feature_gift_impl.presentation.confirm.CreateGiftConfirmPayload

@Subcomponent(
    modules = [
        CreateGiftConfirmModule::class
    ]
)
@ScreenScope
interface CreateGiftConfirmComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: CreateGiftConfirmPayload
        ): CreateGiftConfirmComponent
    }

    fun inject(fragment: CreateGiftConfirmFragment)
}

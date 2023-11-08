package io.novafoundation.nova.feature_assets.presentation.send.amount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.send.amount.SelectSendFragment
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload

@Subcomponent(
    modules = [
        SelectSendModule::class
    ]
)
@ScreenScope
interface SelectSendComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance recipientAddress: String?,
            @BindsInstance payload: SendPayload
        ): SelectSendComponent
    }

    fun inject(fragment: SelectSendFragment)
}

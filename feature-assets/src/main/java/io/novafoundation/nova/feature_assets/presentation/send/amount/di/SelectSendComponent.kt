package io.novafoundation.nova.feature_assets.presentation.send.amount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.send.amount.SelectSendFragment

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
            @BindsInstance payload: AssetPayload
        ): SelectSendComponent
    }

    fun inject(fragment: SelectSendFragment)
}

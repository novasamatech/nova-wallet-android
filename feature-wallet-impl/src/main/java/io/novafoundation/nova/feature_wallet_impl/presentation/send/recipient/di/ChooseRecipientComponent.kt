package io.novafoundation.nova.feature_wallet_impl.presentation.send.recipient.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.send.recipient.ChooseRecipientFragment

@Subcomponent(
    modules = [
        ChooseRecipientModule::class
    ]
)
@ScreenScope
interface ChooseRecipientComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance assetPayload: AssetPayload,
        ): ChooseRecipientComponent
    }

    fun inject(fragment: ChooseRecipientFragment)
}

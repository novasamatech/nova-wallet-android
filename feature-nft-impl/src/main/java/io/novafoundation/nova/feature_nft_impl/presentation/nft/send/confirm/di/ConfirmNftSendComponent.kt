package io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.NftTransferDraft
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm.ConfirmNftSendFragment

@Subcomponent(
    modules = [
        ConfirmNftSendModule::class
    ]
)
@ScreenScope
interface ConfirmNftSendComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: NftTransferDraft
        ): ConfirmNftSendComponent
    }

    fun inject(fragment: ConfirmNftSendFragment)
}

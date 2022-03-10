package io.novafoundation.nova.feature_assets.presentation.send.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.confirm.ConfirmTransferFragment

@Subcomponent(
    modules = [
        ConfirmTransferModule::class
    ]
)
@ScreenScope
interface ConfirmTransferComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance transferDraft: TransferDraft
        ): ConfirmTransferComponent
    }

    fun inject(fragment: ConfirmTransferFragment)
}

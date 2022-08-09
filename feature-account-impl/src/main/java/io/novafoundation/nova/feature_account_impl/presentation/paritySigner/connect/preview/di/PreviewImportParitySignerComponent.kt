package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview.PreviewImportParitySignerFragment

@Subcomponent(
    modules = [
        PreviewImportParitySignerModule::class
    ]
)
@ScreenScope
interface PreviewImportParitySignerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ParitySignerAccountPayload
        ): PreviewImportParitySignerComponent
    }

    fun inject(fragment: PreviewImportParitySignerFragment)
}

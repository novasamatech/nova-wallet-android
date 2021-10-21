package io.novafoundation.nova.feature_account_impl.presentation.exporting.mnemonic.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.mnemonic.ExportMnemonicFragment

@Subcomponent(
    modules = [
        ExportMnemonicModule::class
    ]
)
@ScreenScope
interface ExportMnemonicComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ExportPayload
        ): ExportMnemonicComponent
    }

    fun inject(fragment: ExportMnemonicFragment)
}

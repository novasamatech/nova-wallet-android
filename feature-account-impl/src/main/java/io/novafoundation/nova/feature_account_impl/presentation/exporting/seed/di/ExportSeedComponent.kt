package io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.ExportSeedFragment

@Subcomponent(
    modules = [
        ExportSeedModule::class
    ]
)
@ScreenScope
interface ExportSeedComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ExportPayload.ChainAccount
        ): ExportSeedComponent
    }

    fun inject(fragment: ExportSeedFragment)
}

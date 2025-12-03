package io.novafoundation.nova.feature_account_impl.presentation.seedScan.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.seedScan.ScanSeedFragment

@Subcomponent(
    modules = [
        ScanSeedModule::class
    ]
)
@ScreenScope
interface ScanSeedComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ScanSeedComponent
    }

    fun inject(fragment: ScanSeedFragment)
}

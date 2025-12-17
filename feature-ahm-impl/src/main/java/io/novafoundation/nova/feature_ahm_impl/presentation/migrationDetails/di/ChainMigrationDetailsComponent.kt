package io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.ChainMigrationDetailsFragment
import io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.ChainMigrationDetailsPayload

@Subcomponent(
    modules = [
        ChainMigrationDetailsModule::class
    ]
)
@ScreenScope
interface ChainMigrationDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ChainMigrationDetailsPayload
        ): ChainMigrationDetailsComponent
    }

    fun inject(fragment: ChainMigrationDetailsFragment)
}

package io.novafoundation.nova.feature_account_migration.presentation.pairing.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_migration.presentation.pairing.AccountMigrationPairingFragment
import io.novafoundation.nova.feature_account_migration.presentation.pairing.AccountMigrationPairingPayload

@Subcomponent(
    modules = [
        AccountMigrationPairingModule::class
    ]
)
@ScreenScope
interface AccountMigrationPairingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AccountMigrationPairingPayload
        ): AccountMigrationPairingComponent
    }

    fun inject(fragment: AccountMigrationPairingFragment)
}

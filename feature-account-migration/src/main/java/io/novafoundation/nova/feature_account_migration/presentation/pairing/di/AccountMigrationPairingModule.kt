package io.novafoundation.nova.feature_account_migration.presentation.pairing.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_migration.domain.AccountMigrationInteractor
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_account_migration.presentation.pairing.AccountMigrationPairingPayload
import io.novafoundation.nova.feature_account_migration.presentation.pairing.AccountMigrationPairingViewModel
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory

@Module(includes = [ViewModelModule::class])
class AccountMigrationPairingModule {

    @Provides
    @IntoMap
    @ViewModelKey(AccountMigrationPairingViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        accountMigrationMixinProvider: AccountMigrationMixinProvider,
        accountMigrationInteractor: AccountMigrationInteractor,
        payload: AccountMigrationPairingPayload,
        router: AccountMigrationRouter,
        cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory
    ): ViewModel {
        return AccountMigrationPairingViewModel(
            resourceManager,
            accountMigrationMixinProvider,
            accountMigrationInteractor,
            payload,
            router,
            cloudBackupChangingWarningMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): AccountMigrationPairingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AccountMigrationPairingViewModel::class.java)
    }
}

package io.novafoundation.nova.feature_account_migration.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_cloud_backup_api.di.CloudBackupFeatureApi
import javax.inject.Inject

@ApplicationScope
class AccountMigrationFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: AccountMigrationRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val featureDependencies = DaggerAccountMigrationFeatureComponent_AccountMigrationFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .cloudBackupFeatureApi(getFeature(CloudBackupFeatureApi::class.java))
            .build()

        return DaggerAccountMigrationFeatureComponent.factory()
            .create(featureDependencies, router)
    }
}

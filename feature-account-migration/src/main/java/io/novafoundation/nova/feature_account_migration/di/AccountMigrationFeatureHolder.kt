package io.novafoundation.nova.feature_account_migration.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import javax.inject.Inject

@ApplicationScope
class AccountMigrationFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val featureDependencies = DaggerAccountMigrationFeatureComponent_AccountMigrationFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .build()

        return DaggerAccountMigrationFeatureComponent.factory()
            .create(featureDependencies)
    }
}

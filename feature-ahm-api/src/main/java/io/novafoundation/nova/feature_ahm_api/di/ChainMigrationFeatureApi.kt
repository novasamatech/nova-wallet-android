package io.novafoundation.nova.feature_ahm_api.di

import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.di.deeplinks.ChainMigrationDeepLinks
import io.novafoundation.nova.feature_ahm_api.domain.AssetMigrationUseCase
import io.novafoundation.nova.feature_ahm_api.domain.StakingMigrationUseCase

interface ChainMigrationFeatureApi {

    val assetMigrationUseCase: AssetMigrationUseCase

    val chainMigrationRepository: ChainMigrationRepository

    val migrationInfoRepository: MigrationInfoRepository

    val chainMigrationDeepLinks: ChainMigrationDeepLinks

    val stakingMigrationUseCase: StakingMigrationUseCase
}

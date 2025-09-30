package io.novafoundation.nova.feature_ahm_api.di

import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.di.deeplinks.ChainMigrationDeepLinks
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationDetailsSelectToShowUseCase

interface ChainMigrationFeatureApi {

    fun chainMigrationRepository(): ChainMigrationRepository

    fun migrationInfoRepository(): MigrationInfoRepository

    fun chainMigrationDeepLinks(): ChainMigrationDeepLinks

    val chainMigrationDetailsSelectToShowUseCase: ChainMigrationDetailsSelectToShowUseCase
}

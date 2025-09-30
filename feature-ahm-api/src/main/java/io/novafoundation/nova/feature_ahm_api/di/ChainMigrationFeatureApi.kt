package io.novafoundation.nova.feature_ahm_api.di

import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.di.deeplinks.ChainMigrationDeepLinks
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationInfoUseCase
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationDetailsSelectToShowUseCase

interface ChainMigrationFeatureApi {

    val chainMigrationInfoUseCase: ChainMigrationInfoUseCase

    val chainMigrationRepository: ChainMigrationRepository

    val migrationInfoRepository: MigrationInfoRepository

    val chainMigrationDeepLinks: ChainMigrationDeepLinks

    val chainMigrationDetailsSelectToShowUseCase: ChainMigrationDetailsSelectToShowUseCase
}

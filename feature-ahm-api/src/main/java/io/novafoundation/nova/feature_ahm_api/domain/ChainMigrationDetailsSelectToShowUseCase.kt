package io.novafoundation.nova.feature_ahm_api.domain

interface ChainMigrationDetailsSelectToShowUseCase {
    suspend fun getChainIdsToShowMigrationDetails(): List<String>
}

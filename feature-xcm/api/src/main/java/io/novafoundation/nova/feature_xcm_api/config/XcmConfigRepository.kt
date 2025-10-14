package io.novafoundation.nova.feature_xcm_api.config

import io.novafoundation.nova.feature_xcm_api.config.model.GeneralXcmConfig
import kotlinx.coroutines.flow.Flow

interface XcmConfigRepository {

    suspend fun awaitXcmConfig(): GeneralXcmConfig

    fun xcmConfigFlow(): Flow<GeneralXcmConfig>
}

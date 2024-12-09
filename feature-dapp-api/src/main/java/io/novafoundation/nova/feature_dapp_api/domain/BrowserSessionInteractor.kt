package io.novafoundation.nova.feature_dapp_api.domain

import kotlinx.coroutines.flow.Flow

interface BrowserSessionInteractor {

    fun destroyActiveSessionsOnAccountChange(): Flow<Unit>
}

package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.core_db.dao.MetaAccountSettingsDao
import io.novafoundation.nova.core_db.model.MetaAccountSettingsLocal
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AutoEnableTokensRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// On by default: discovery is what makes "show what you hold" work at all, and a wallet nobody has
// changed anything on has no row to say otherwise.
private const val AUTO_ENABLE_TOKENS_DEFAULT = true

class RealAutoEnableTokensRepository(
    private val dao: MetaAccountSettingsDao
) : AutoEnableTokensRepository {

    override suspend fun autoEnableTokens(metaId: Long): Boolean {
        return dao.getAutoAddTokensWithBalance(metaId) ?: AUTO_ENABLE_TOKENS_DEFAULT
    }

    override fun autoEnableTokensFlow(metaId: Long): Flow<Boolean> {
        return dao.observeAutoAddTokensWithBalance(metaId).map { it ?: AUTO_ENABLE_TOKENS_DEFAULT }
    }

    override suspend fun setAutoEnableTokens(metaId: Long, enabled: Boolean) {
        dao.setSettings(MetaAccountSettingsLocal(metaId = metaId, autoAddTokensWithBalance = enabled))
    }
}

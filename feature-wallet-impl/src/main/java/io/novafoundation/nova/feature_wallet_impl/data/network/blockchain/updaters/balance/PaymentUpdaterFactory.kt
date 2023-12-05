package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealPaymentUpdaterFactory(
    private val operationDao: OperationDao,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val scope: AccountUpdateScope,
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache
) : PaymentUpdaterFactory {

    override fun createFullSync(chain: Chain): Updater<MetaAccount> {
        return FullSyncPaymentUpdater(
            operationDao = operationDao,
            assetSourceRegistry = assetSourceRegistry,
            scope = scope,
            chain = chain,
        )
    }

    override fun createLightSync(chain: Chain): Updater<MetaAccount> {
        return LightSyncPaymentUpdater(
            scope = scope,
            chainRegistry = chainRegistry,
            chain = chain,
            assetCache = assetCache
        )
    }
}

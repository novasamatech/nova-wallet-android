package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException

/** Captured before handoff; changing the selected wallet or backend cannot invalidate another operation's data. */
data class BittensorCacheInvalidation(
    val history: BittensorHistoryRequest,
    val chainId: String,
    val coldkey: String
)

class BittensorCacheInvalidator(
    private val cache: BittensorResponseCache,
    private val history: BittensorOperationHistoryRepository,
    private val accounts: AccountRepository,
    private val logger: Logger
) {

    suspend fun capture(metaId: Long, chain: Chain): BittensorCacheInvalidation? = try {
        val account = accounts.getMetaAccount(metaId).requireAccountIdIn(chain)
        history.request(chain.addressOf(account))?.let { BittensorCacheInvalidation(it, chain.id, account.toHexString()) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.log("[BittensorCache] Could not capture transaction cache scope")
        logger.log(e)
        null
    }

    suspend fun submitted(request: BittensorCacheInvalidation?) {
        if (request == null) return
        try {
            history.ownTradeSubmitted(request.history)
            cache.invalidate(
                request.history.baseUrl,
                request.coldkey,
                listOf("runtime/positions/${request.chainId}", "runtime/prices/${request.chainId}"),
                keepStale = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Cache IO cannot change an already observed dispatch result.
            logger.log("[BittensorCache] Failed to invalidate transaction snapshots")
            logger.log(e)
        }
    }
}

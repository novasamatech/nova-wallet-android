package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.security.MessageDigest
import okhttp3.HttpUrl

internal object BittensorCacheKeys {

    fun portfolioFx(priceId: String, currency: String, period: String) =
        "bittensor://portfolio-fx/${bittensorCacheHash("$priceId|$currency|$period")}"

    fun positions(chainId: String, coldkey: String) = bittensorSnapshotKey("runtime/positions/$chainId", coldkey)

    fun prices(chainId: String, coldkey: String) = bittensorSnapshotKey("runtime/prices/$chainId", coldkey)

    fun dashboard(chainId: String, coldkey: AccountId) = bittensorSnapshotKey("dashboard/$chainId", coldkey.toHexString())

    fun claimableRewards(chainId: String, coldkey: AccountId) = bittensorSnapshotKey("claimable-rewards/$chainId", coldkey.toHexString())
    fun sharedPositions(chainId: String, coldkey: AccountId, metaAccountId: Long) = sharedKey("Positions", chainId, coldkey, metaAccountId)

    fun sharedPrices(chainId: String, coldkey: AccountId, metaAccountId: Long) = sharedKey("Prices", chainId, coldkey, metaAccountId)

    private fun sharedKey(component: String, chainId: String, coldkey: AccountId, metaAccountId: Long): String {
        // ComputationalCache logs keys; never include the raw account identifier.
        val accountKey = coldkey.blake2b256().toHexString()
        return "Bittensor.$component.$metaAccountId.$chainId.$accountKey"
    }
}

/** Local chain identity; resolving it never requires a backend URL or network request. */
private fun bittensorSnapshotKey(path: String, coldkey: String) = "bittensor://$path?coldkey=${bittensorCacheHash(coldkey)}"

internal fun bittensorGenerationKey(base: HttpUrl) = "bittensor://generation/${bittensorCacheHash(base.toString())}"

private fun bittensorCacheHash(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import retrofit2.http.GET
import retrofit2.http.Url
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

interface BittensorEarnConfigApi {

    @GET
    suspend fun getConfig(@Url url: String): BittensorEarnConfigRemote
}

class BittensorEarnConfigRemote(
    val entry: Entry?,
    val preferredRootValidator: String?
) {
    class Entry(val enabled: Boolean)
}

class RemoteBittensorRootValidatorSource(
    api: BittensorEarnConfigApi,
    configUrl: String,
    private val backend: BittensorValidatorBackend,
    private val logger: Logger,
    private val selection: BittensorRootValidatorSelection
) : BittensorRootValidatorSource {

    private val config = SingleValueCache { api.getConfig(configUrl) }

    override suspend fun preferredValidator(chain: Chain): BittensorValidator {
        val config = config()
        check(config.entry?.enabled == true) { "Bittensor Earn is not enabled" }
        val address = requireNotNull(selection.address.value ?: config.preferredRootValidator) { "Preferred root validator is not configured" }

        return BittensorValidator(
            hotkey = AccountIdKey(chain.accountIdOf(address)),
            name = metrics(address)?.validator?.name?.takeIf(String::isNotBlank) ?: address,
            iconUrl = null
        )
    }

    override suspend fun annualPercentageYield(): BigDecimal? {
        val address = selection.address.value ?: config().preferredRootValidator ?: return null
        val window = metrics(address)?.validator?.stakeYield?.h24 ?: return null
        if (window.coverage !in setOf("FULL", "PARTIAL") || window.valueQuality !in setOf("REPORTED", "DERIVED")) return null
        return window.apy?.toBigDecimalOrNull()?.takeIf { it > BigDecimal("-100") }
    }

    private suspend fun metrics(address: String): BittensorRootValidatorRemote? = try {
        backend.rootValidator(address)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.log("[BittensorRootYield] Validator metrics unavailable")
        logger.log(e)
        null
    }
}

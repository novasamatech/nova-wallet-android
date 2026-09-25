package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import retrofit2.http.GET
import retrofit2.http.Url
import java.math.BigDecimal

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
    configUrl: String
) : BittensorRootValidatorSource {

    private val config = SingleValueCache { api.getConfig(configUrl) }

    override suspend fun preferredValidator(chain: Chain): BittensorValidator {
        val config = config()
        check(config.entry?.enabled == true) { "Bittensor Earn is not enabled" }
        val address = requireNotNull(config.preferredRootValidator) { "Preferred root validator is not configured" }

        return BittensorValidator(
            hotkey = AccountIdKey(chain.accountIdOf(address)),
            name = address,
            iconUrl = null
        )
    }

    override suspend fun annualPercentageYield(): BigDecimal? = null
}

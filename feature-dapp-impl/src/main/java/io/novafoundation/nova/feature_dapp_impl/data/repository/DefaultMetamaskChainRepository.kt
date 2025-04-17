package io.novafoundation.nova.feature_dapp_impl.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import javax.inject.Inject

interface DefaultMetamaskChainRepository {

    fun getDefaultMetamaskChain(): MetamaskChain?

    fun saveDefaultMetamaskChain(chain: MetamaskChain)
}

private const val PREFERENCES_KEY = "RealDefaultMetamaskChainRepository.DefaultMetamaskChain"

@FeatureScope
class RealDefaultMetamaskChainRepository @Inject constructor(
    private val preferences: Preferences,
    private val gson: Gson,
) : DefaultMetamaskChainRepository {

    override fun getDefaultMetamaskChain(): MetamaskChain? {
        val raw = preferences.getString(PREFERENCES_KEY) ?: return null
        return runCatching { gson.fromJson<MetamaskChain>(raw) }.getOrNull()
    }

    override fun saveDefaultMetamaskChain(chain: MetamaskChain) {
        val raw = gson.toJson(chain)
        preferences.putString(PREFERENCES_KEY, raw)
    }
}

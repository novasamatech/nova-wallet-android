package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.historical

import io.novafoundation.nova.common.utils.staking
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class HistoricalTotalValidatorRewardUpdater : HistoricalUpdater {

    override fun constructKeyPrefix(runtime: RuntimeSnapshot): String {
        return runtime.metadata.staking().storage("ErasValidatorReward").storageKey(runtime)
    }
}

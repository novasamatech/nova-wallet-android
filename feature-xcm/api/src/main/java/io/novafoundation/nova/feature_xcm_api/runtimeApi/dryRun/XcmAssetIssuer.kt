package io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

interface XcmAssetIssuer {

    suspend fun issueAssetsCall(
        asset: Chain.Asset,
        amount: BalanceOf,
        destination: AccountIdKey
    ): GenericCall.Instance
}

package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_xcm_api.dryRun.model.OriginCaller
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

interface AssetIssuer {

    /**
     * Compose a call to issue [amount] of tokens to [destination]
     * Implementation can assume execution happens under [OriginCaller.System.Root]
     */
    suspend fun composeIssueCall(amount: Balance, destination: AccountIdKey): GenericCall.Instance
}

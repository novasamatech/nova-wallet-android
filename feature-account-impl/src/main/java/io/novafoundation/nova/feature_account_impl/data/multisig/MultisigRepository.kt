package io.novafoundation.nova.feature_account_impl.data.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.api.FindMultisigsApi
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.FindMultisigsRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.OffChainPendingMultisigInfoRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.AccountMultisigRemote
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.FindMultisigsResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.GetPedingMultisigOperationsResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.GetPedingMultisigOperationsResponse.OperationRemote
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.feature_account_impl.data.multisig.model.DiscoveredMultisig
import io.novafoundation.nova.feature_account_impl.data.multisig.model.OffChainPendingMultisigOperationInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface MultisigRepository {

    fun supportsMultisigSync(chain: Chain): Boolean

    suspend fun findMultisigAccounts(accountIds: Set<AccountIdKey>): List<DiscoveredMultisig>

    suspend fun getPendingOperationIds(chain: Chain, accountIdKey: AccountIdKey): Set<CallHash>

    suspend fun subscribePendingOperations(
        chain: Chain,
        accountIdKey: AccountIdKey,
        operationIds: Collection<CallHash>
    ): Flow<Map<CallHash, OnChainMultisig?>>

    suspend fun getOffChainPendingOperationsInfo(
        chain: Chain,
        accountId: AccountIdKey,
        pendingCallHashes: Collection<CallHash>
    ): Result<Map<CallHash, OffChainPendingMultisigOperationInfo>>
}

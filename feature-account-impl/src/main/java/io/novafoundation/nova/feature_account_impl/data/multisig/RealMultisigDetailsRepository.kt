package io.novafoundation.nova.feature_account_impl.data.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigDetailsRepository
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisig
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisigs
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Inject
import javax.inject.Named

@FeatureScope
class RealMultisigDetailsRepository @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE) private val remoteStorageSource: StorageDataSource
) : MultisigDetailsRepository {

    override suspend fun getApprovals(chain: Chain, accountIdKey: AccountIdKey, callHash: CallHash): List<AccountIdKey>? {
        return getOnChainMultisig(chain, accountIdKey, callHash)?.approvals
    }

    override suspend fun hasMultisigOperation(chain: Chain, accountIdKey: AccountIdKey, callHash: CallHash): Boolean {
        return getOnChainMultisig(chain, accountIdKey, callHash) != null
    }

    private suspend fun getOnChainMultisig(chain: Chain, accountIdKey: AccountIdKey, operationId: CallHash): OnChainMultisig? {
        return remoteStorageSource.query(chain.id) {
            runtime.metadata.multisig.multisigs.query(accountIdKey, operationId)
        }
    }
}

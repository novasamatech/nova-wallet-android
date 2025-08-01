package io.novafoundation.nova.feature_account_impl.data.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigApprovalsRepository
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisig
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisigs
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Inject
import javax.inject.Named

@FeatureScope
class RealMultisigApprovalsRepository @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE) private val remoteStorageSource: StorageDataSource,
) : MultisigApprovalsRepository {

    override suspend fun getApprovals(chain: Chain, accountIdKey: AccountIdKey, operationId: CallHash): List<AccountIdKey>? {
        return remoteStorageSource.query(chain.id) {
            val result = runtime.metadata.multisig.multisigs.query(accountIdKey, operationId)
            result?.approvals
        }
    }
}

package io.novafoundation.nova.feature_account_impl.data.multisig.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.multisig
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigValidationsRepository
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisig
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.multisigs
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Inject
import javax.inject.Named

@FeatureScope
class RealMultisigValidationsRepository @Inject constructor(
    private val chainRegistry: ChainRegistry,
    @Named(REMOTE_STORAGE_SOURCE) private val storageDataSource: StorageDataSource
) : MultisigValidationsRepository {

    override suspend fun getMultisigDepositBase(chainId: ChainId): BalanceOf {
        return chainRegistry.withRuntime(chainId) {
            metadata.multisig().numberConstant("DepositBase")
        }
    }

    override suspend fun getMultisigDepositFactor(chainId: ChainId): BalanceOf {
        return chainRegistry.withRuntime(chainId) {
            metadata.multisig().numberConstant("DepositFactor")
        }
    }

    override suspend fun hasPendingCallHash(chainId: ChainId, accountIdKey: AccountIdKey, callHash: CallHash): Boolean {
        val pendingCallHash = storageDataSource.query(chainId) {
            metadata.multisig.multisigs.query(accountIdKey, callHash)
        }

        return pendingCallHash != null
    }
}

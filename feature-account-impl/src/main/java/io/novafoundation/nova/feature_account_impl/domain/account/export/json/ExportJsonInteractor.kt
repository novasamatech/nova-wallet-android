package io.novafoundation.nova.feature_account_impl.domain.account.export.json

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class ExportJsonInteractor(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun generateRestoreJson(
        metaId: Long,
        chainId: ChainId,
        password: String,
    ): Result<String> {
        val metaAccount = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(chainId)

        return runCatching {
            accountRepository.generateRestoreJson(metaAccount, chain, password)
        }
    }

    suspend fun generateRestoreJson(
        metaId: Long,
        password: String,
    ): Result<String> {
        val metaAccount = accountRepository.getMetaAccount(metaId)

        return runCatching {
            accountRepository.generateRestoreJson(metaAccount, password)
        }
    }
}

package io.novafoundation.nova.feature_account_api.presenatation.account

import io.novafoundation.nova.common.presentation.toShortAddressFormat
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddressDisplayUseCase(
    private val accountRepository: AccountRepository,
) {

    class Identifier(private val addressToName: Map<String, String?>) {

        fun nameOrAddress(address: String): String {
            return addressToName[address] ?: address.toShortAddressFormat()
        }
    }

    @Deprecated("Does not work with Meta Accounts. Use `invoke(chain: Chain, address: String)` instead")
    // TODO remove
    suspend operator fun invoke(address: String): String? = withContext(Dispatchers.Default) {
        accountRepository.getAccountOrNull(address)?.name
    }

    suspend operator fun invoke(accountId: AccountId): String? = withContext(Dispatchers.Default) {
        accountRepository.findMetaAccount(accountId)?.name
    }

    suspend fun createIdentifier(): Identifier = withContext(Dispatchers.Default) {
        val accounts = accountRepository.getAccounts().associateBy(
            keySelector = { it.address },
            valueTransform = { it.name }
        )

        Identifier(accounts)
    }
}

suspend operator fun AddressDisplayUseCase.invoke(chain: Chain, address: String): String? {
    return runCatching { invoke(chain.accountIdOf(address)) }.getOrNull()
}

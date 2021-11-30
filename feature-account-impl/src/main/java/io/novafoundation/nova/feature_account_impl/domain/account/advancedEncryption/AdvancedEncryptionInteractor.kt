package io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption

import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.utils.DEFAULT_DERIVATION_PATH
import io.novafoundation.nova.common.utils.input.disabledInput
import io.novafoundation.nova.common.utils.input.modifiableInput
import io.novafoundation.nova.common.utils.input.unmodifiableInput
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.encrypt.EncryptionType
import jp.co.soramitsu.fearless_utils.encrypt.MultiChainEncryption
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder

private val DEFAULT_SUBSTRATE_ENCRYPTION = mapEncryptionToCryptoType(EncryptionType.SR25519)
private val ETHEREUM_ENCRYPTION = mapEncryptionToCryptoType(MultiChainEncryption.Ethereum.encryptionType)

private const val DEFAULT_SUBSTRATE_DERIVATION_PATH = ""
private val ETHEREUM_DEFAULT_DERIVATION_PATH = BIP32JunctionDecoder.DEFAULT_DERIVATION_PATH

class AdvancedEncryptionInteractor(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) {

    fun getCryptoTypes(): List<CryptoType> {
        return accountRepository.getEncryptionTypes()
    }

    suspend fun getInitialInputState(chainId: ChainId?): AdvancedEncryptionInput {
        return if (chainId != null) {
            val chain = chainRegistry.getChain(chainId)

            if (chain.isEthereumBased) { // Ethereum Chain Account
                AdvancedEncryptionInput(
                    substrateCryptoType = disabledInput(),
                    substrateDerivationPath = disabledInput(),
                    ethereumCryptoType = ETHEREUM_ENCRYPTION.unmodifiableInput(),
                    ethereumDerivationPath = ETHEREUM_DEFAULT_DERIVATION_PATH.modifiableInput()
                )
            } else { // Substrate Chain Account
                AdvancedEncryptionInput(
                    substrateCryptoType = DEFAULT_SUBSTRATE_ENCRYPTION.modifiableInput(),
                    substrateDerivationPath = DEFAULT_SUBSTRATE_DERIVATION_PATH.modifiableInput(),
                    ethereumCryptoType = disabledInput(),
                    ethereumDerivationPath = disabledInput()
                )
            }
        } else { // MetaAccount
            AdvancedEncryptionInput(
                substrateCryptoType = DEFAULT_SUBSTRATE_ENCRYPTION.modifiableInput(),
                substrateDerivationPath = DEFAULT_SUBSTRATE_DERIVATION_PATH.modifiableInput(),
                ethereumCryptoType = ETHEREUM_ENCRYPTION.unmodifiableInput(),
                ethereumDerivationPath = ETHEREUM_DEFAULT_DERIVATION_PATH.modifiableInput()
            )
        }
    }
}

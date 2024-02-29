package io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.derivationPath
import io.novafoundation.nova.common.data.secrets.v2.ethereumDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.substrateDerivationPath
import io.novafoundation.nova.common.utils.fold
import io.novafoundation.nova.common.utils.input.Input
import io.novafoundation.nova.common.utils.input.disabledInput
import io.novafoundation.nova.common.utils.input.modifiableInput
import io.novafoundation.nova.common.utils.input.unmodifiableInput
import io.novafoundation.nova.common.utils.nullIfEmpty
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.secrets.getAccountSecrets
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryptionInput
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.chainAccountFor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.chainIdOrNull
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.presenatation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

private typealias DerivationPathModifier = String?.() -> Input<String>

class AdvancedEncryptionInteractor(
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
    private val encryptionDefaults: EncryptionDefaults
) {

    fun getCryptoTypes(): List<CryptoType> {
        return accountRepository.getEncryptionTypes()
    }

    suspend fun getRecommendedAdvancedEncryption(): AdvancedEncryption {
        return AdvancedEncryption(
            substrateCryptoType = encryptionDefaults.substrateCryptoType,
            ethereumCryptoType = encryptionDefaults.ethereumCryptoType,
            derivationPaths = AdvancedEncryption.DerivationPaths(
                substrate = encryptionDefaults.substrateDerivationPath,
                ethereum = encryptionDefaults.ethereumDerivationPath
            )
        )
    }

    suspend fun getInitialInputState(payload: AdvancedEncryptionModePayload): AdvancedEncryptionInput {
        return when (payload) {
            is AdvancedEncryptionModePayload.Change -> getChangeInitialInputState(payload.addAccountPayload.chainIdOrNull)
            is AdvancedEncryptionModePayload.View -> getViewInitialInputState(payload.metaAccountId, payload.chainId, payload.hideDerivationPaths)
        }
    }

    private suspend fun getViewInitialInputState(
        metaAccountId: Long,
        chainId: ChainId,
        hideDerivationPaths: Boolean
    ): AdvancedEncryptionInput {
        val chain = chainRegistry.getChain(chainId)
        val metaAccount = accountRepository.getMetaAccount(metaAccountId)

        val accountSecrets = secretStoreV2.getAccountSecrets(metaAccount, chain)

        val derivationPathModifier = derivationPathModifier(hideDerivationPaths)

        return accountSecrets.fold(
            left = { metaAccountSecrets ->
                if (chain.isEthereumBased) {
                    AdvancedEncryptionInput(
                        substrateCryptoType = disabledInput(),
                        substrateDerivationPath = disabledInput(),
                        ethereumCryptoType = encryptionDefaults.ethereumCryptoType.asReadOnlyInput(),
                        ethereumDerivationPath = metaAccountSecrets.ethereumDerivationPath.derivationPathModifier()
                    )
                } else {
                    AdvancedEncryptionInput(
                        substrateCryptoType = metaAccount.substrateCryptoType.asReadOnlyInput(),
                        substrateDerivationPath = metaAccountSecrets.substrateDerivationPath.derivationPathModifier(),
                        ethereumCryptoType = disabledInput(),
                        ethereumDerivationPath = disabledInput()
                    )
                }
            },
            right = { chainAccountSecrets ->
                if (chain.isEthereumBased) {
                    AdvancedEncryptionInput(
                        substrateCryptoType = disabledInput(),
                        substrateDerivationPath = disabledInput(),
                        ethereumCryptoType = encryptionDefaults.ethereumCryptoType.asReadOnlyInput(),
                        ethereumDerivationPath = chainAccountSecrets.derivationPath.derivationPathModifier()
                    )
                } else {
                    val chainAccount = metaAccount.chainAccountFor(chainId)

                    AdvancedEncryptionInput(
                        substrateCryptoType = chainAccount.cryptoType.asReadOnlyInput(),
                        substrateDerivationPath = chainAccountSecrets.derivationPath.derivationPathModifier(),
                        ethereumCryptoType = disabledInput(),
                        ethereumDerivationPath = disabledInput()
                    )
                }
            }
        )
    }

    private suspend fun getChangeInitialInputState(chainId: ChainId?) = if (chainId != null) {
        val chain = chainRegistry.getChain(chainId)

        if (chain.isEthereumBased) { // Ethereum Chain Account
            AdvancedEncryptionInput(
                substrateCryptoType = disabledInput(),
                substrateDerivationPath = disabledInput(),
                ethereumCryptoType = encryptionDefaults.ethereumCryptoType.unmodifiableInput(),
                ethereumDerivationPath = encryptionDefaults.ethereumDerivationPath.modifiableInput()
            )
        } else { // Substrate Chain Account
            AdvancedEncryptionInput(
                substrateCryptoType = encryptionDefaults.substrateCryptoType.modifiableInput(),
                substrateDerivationPath = encryptionDefaults.substrateDerivationPath.modifiableInput(),
                ethereumCryptoType = disabledInput(),
                ethereumDerivationPath = disabledInput()
            )
        }
    } else { // MetaAccount
        AdvancedEncryptionInput(
            substrateCryptoType = encryptionDefaults.substrateCryptoType.modifiableInput(),
            substrateDerivationPath = encryptionDefaults.substrateDerivationPath.modifiableInput(),
            ethereumCryptoType = encryptionDefaults.ethereumCryptoType.unmodifiableInput(),
            ethereumDerivationPath = encryptionDefaults.ethereumDerivationPath.modifiableInput()
        )
    }

    private fun <T> Input<T>.hideIf(condition: Boolean) = if (condition) {
        Input.Disabled
    } else {
        this
    }

    private fun derivationPathModifier(hideDerivationPaths: Boolean): DerivationPathModifier = {
        asReadOnlyInput().hideIf(hideDerivationPaths)
    }

    private fun <T> T?.asReadOnlyInput() = this?.unmodifiableInput() ?: disabledInput()

    private fun String?.asReadOnlyStringInput() = this?.nullIfEmpty().asReadOnlyInput()
}

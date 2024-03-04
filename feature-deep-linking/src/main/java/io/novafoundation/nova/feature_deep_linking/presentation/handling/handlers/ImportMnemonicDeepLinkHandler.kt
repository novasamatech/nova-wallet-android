package io.novafoundation.nova.app.root.presentation.deepLinks.handlers

import android.net.Uri
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException.ImportMnemonicHandlingException
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.derivationPath.DerivationPathDecoder
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType
import io.novafoundation.nova.feature_account_api.presenatation.account.common.model.AdvancedEncryptionModel
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.Mnemonic
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.MnemonicCreator
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val IMPORT_WALLET_DEEP_LINK_PREFIX = "/create/wallet"

class ImportMnemonicDeepLinkHandler(
    private val router: DeepLinkingRouter,
    private val encryptionDefaults: EncryptionDefaults,
    private val accountRepository: AccountRepository,
    private val automaticInteractionGate: AutomaticInteractionGate,
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(IMPORT_WALLET_DEEP_LINK_PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri) {
        if (accountRepository.hasActiveMetaAccounts()) {
            automaticInteractionGate.awaitInteractionAllowed()
        }

        val mnemonic = data.getMnemonic()
        val substrateDP = data.getSubstrateDP()
        val ethereumDerivationPath = data.getEthereumDP()

        val isDerivationPathsValid = isDerivationPathsValid(substrateDP, ethereumDerivationPath)
        if (!isDerivationPathsValid) throw ImportMnemonicHandlingException.InvalidDerivationPath

        val importAccountPayload = ImportAccountPayload(
            prepareMnemonicPreset(
                mnemonic = mnemonic.words,
                substrateCryptoType = data.getSubstrateCryptoType().asCryptoType { encryptionDefaults.substrateCryptoType },
                substrateDP = data.getSubstrateDP(),
                ethereumDP = data.getEthereumDP()
            ),
            AddAccountPayload.MetaAccount
        )

        router.openImportAccountScreen(importAccountPayload)
    }

    private fun prepareMnemonicPreset(
        mnemonic: String,
        substrateCryptoType: CryptoType?,
        substrateDP: String?,
        ethereumDP: String?
    ): ImportType.Mnemonic {
        return ImportType.Mnemonic(
            mnemonic = mnemonic,
            preset = AdvancedEncryptionModel(
                substrateCryptoType = substrateCryptoType ?: encryptionDefaults.substrateCryptoType,
                substrateDerivationPath = substrateDP ?: encryptionDefaults.substrateDerivationPath,
                ethereumCryptoType = encryptionDefaults.ethereumCryptoType,
                ethereumDerivationPath = ethereumDP ?: encryptionDefaults.ethereumDerivationPath
            )
        )
    }

    private fun Uri.getMnemonic(): Mnemonic {
        val mnemonicHex = getQueryParameter("mnemonic")
        return runCatching { MnemonicCreator.fromEntropy(mnemonicHex!!.fromHex()) }.getOrNull()
            ?: throw ImportMnemonicHandlingException.InvalidMnemonic
    }

    private fun Uri.getSubstrateCryptoType(): String? {
        return getQueryParameter("cryptoType")
    }

    private fun Uri.getSubstrateDP(): String? {
        return getQueryParameter("substrateDP")
    }

    private fun Uri.getEthereumDP(): String? {
        return getQueryParameter("evmDP")
    }

    private fun String?.asCryptoType(fallback: () -> CryptoType): CryptoType {
        val intCryptoType = this?.toIntOrNull()

        return when (intCryptoType) {
            null -> fallback()
            0 -> CryptoType.SR25519
            1 -> CryptoType.ED25519
            2 -> CryptoType.ECDSA
            else -> throw DeepLinkHandlingException.ImportMnemonicHandlingException.InvalidCryptoType
        }
    }

    private fun isDerivationPathsValid(substrateDP: String?, ethereumDP: String?): Boolean {
        return DerivationPathDecoder.isEthereumDerivationPathValid(ethereumDP) &&
            DerivationPathDecoder.isSubstrateDerivationPathValid(substrateDP)
    }
}

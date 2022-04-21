package io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.sign

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.mappers.mapGradientToUi
import io.novafoundation.nova.feature_account_api.data.secrets.getEthereumKeypair
import io.novafoundation.nova.feature_account_api.data.secrets.signEthereum
import io.novafoundation.nova.feature_account_api.data.secrets.signEthereumPrefixed
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_dapp_impl.data.network.ethereum.EthereumApi
import io.novafoundation.nova.feature_dapp_impl.data.network.ethereum.EthereumApiFactory
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.MetamaskInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DAppSignInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskSendTransactionRequest
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskSendTransactionRequest.Payload
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskTransaction
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.PersonalSignMessage
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.TypedMessage
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAddress
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toAccountId
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.web3j.crypto.RawTransaction
import java.math.BigInteger

class MetamaskSignInteractorFactory(
    private val metamaskInteractor: MetamaskInteractor,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val ethereumApiFactory: EthereumApiFactory,
) {

    fun create(request: MetamaskSendTransactionRequest) = MetamaskSignInteractor(
        chainRegistry = chainRegistry,
        tokenRepository = tokenRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        request = request,
        metamaskInteractor = metamaskInteractor,
        ethereumApiFactory = ethereumApiFactory,
        accountRepository = accountRepository,
        secretStoreV2 = secretStoreV2
    )
}

class MetamaskSignInteractor(
    private val ethereumApiFactory: EthereumApiFactory,
    private val request: MetamaskSendTransactionRequest,
    private val metamaskInteractor: MetamaskInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val tokenRepository: TokenRepository,
    private val chainRegistry: ChainRegistry,
    private val extrinsicGson: Gson,
    private val secretStoreV2: SecretStoreV2,
    private val accountRepository: AccountRepository,
) : DAppSignInteractor {

    private val mostRecentFormedTx = singleReplaySharedFlow<RawTransaction>()

    val payload = request.payload

    private val ethereumApi by lazy {
        val nodeUrl = request.payload.chain.rpcUrls.first()

        ethereumApiFactory.create(nodeUrl)
    }

    override suspend fun createAccountAddressModel(): AddressModel = withContext(Dispatchers.Default) {
        val address = request.payload.originAddress
        val someEthereumChain = chainRegistry.findChain { it.isEthereumBased }!! // always have at least one ethereum chain in the app

        addressIconGenerator.createAccountAddressModel(someEthereumChain, address)
    }

    override suspend fun chainUi(): ChainUi = withContext(Dispatchers.Default) {
        val metamaskChain = request.payload.chain

        metamaskInteractor.tryFindChainFromEthereumChainId(metamaskChain.chainId)?.let(::mapChainToUi)
            ?: mapMetamaskChainToUi(metamaskChain)
    }

    override fun commissionTokenFlow(): Flow<Token>? {
        if (payload !is Payload.SendTx) return null

        return flow {
            val metamaskChain = request.payload.chain
            val chain = metamaskInteractor.tryFindChainFromEthereumChainId(metamaskChain.chainId)

            if (chain != null) {
                emitAll(tokenRepository.observeToken(chain.utilityAsset))
            } else {
                emit(createTokenFrom(metamaskChain))
            }
        }
    }

    override suspend fun calculateFee(): BigInteger = withContext(Dispatchers.Default) {
        if (payload !is Payload.SendTx) return@withContext BigInteger.ZERO

        val tx = ethereumApi.formTransaction(payload.transaction)
        mostRecentFormedTx.emit(tx)

        tx.gasLimit * tx.gasPrice
    }

    override suspend fun performOperation(): DAppSignCommunicator.Response = withContext(Dispatchers.Default) {
        runCatching {
            when (payload) {
                is Payload.SendTx -> sendTx(payload.transaction)
                is Payload.SignTypedMessage -> signTypedMessage(payload.message)
                is Payload.PersonalSign -> personalSign(payload.message)
            }
        }.getOrElse {
            Log.e(LOG_TAG, "Failed to sign tx from Metamask", it)

            DAppSignCommunicator.Response.SigningFailed(request.id)
        }
    }

    override suspend fun readableOperationContent(): String = withContext(Dispatchers.Default) {
        when (payload) {
            is Payload.SendTx -> extrinsicGson.toJson(mostRecentFormedTx.first())
            is Payload.SignTypedMessage -> signTypedMessageReadableContent(payload)
            is Payload.PersonalSign -> personalSignReadableContent(payload)
        }
    }

    override fun shutdown() {
        return ethereumApi.shutdown()
    }

    private suspend fun sendTx(basedOn: MetamaskTransaction): DAppSignCommunicator.Response.Sent {
        val tx = ethereumApi.formTransaction(basedOn)

        val originAccountId = originAccountId()
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val keypair = secretStoreV2.getEthereumKeypair(metaAccount, originAccountId)

        val chainId = request.payload.chain.chainId.removeHexPrefix().toLong(16)

        val txHash = ethereumApi.sendTransaction(tx, keypair, chainId)

        return DAppSignCommunicator.Response.Sent(request.id, txHash)
    }

    private suspend fun signTypedMessage(message: TypedMessage): DAppSignCommunicator.Response.Signed {
        val signature = signMessage(message.data.fromHex())

        return DAppSignCommunicator.Response.Signed(request.id, signature)
    }

    private suspend fun personalSign(message: PersonalSignMessage): DAppSignCommunicator.Response.Signed {
        val messageBytes = message.data.fromHex()

        val signature = secretStoreV2.signEthereumPrefixed(
            metaAccount = accountRepository.getSelectedMetaAccount(),
            accountId = originAccountId(),
            message = messageBytes
        ).toHexString(withPrefix = true)

        return DAppSignCommunicator.Response.Signed(request.id, signature)
    }

    private suspend fun signMessage(message: ByteArray): String {
        return secretStoreV2.signEthereum(
            metaAccount = accountRepository.getSelectedMetaAccount(),
            accountId = originAccountId(),
            message = message
        ).toHexString(withPrefix = true)
    }

    private fun personalSignReadableContent(payload: Payload.PersonalSign): String {
        val data = payload.message.data

        return runCatching { data.fromHex().decodeToString(throwOnInvalidSequence = true) }
            .getOrDefault(data)
    }

    private fun signTypedMessageReadableContent(payload: Payload.SignTypedMessage): String {
        return runCatching {
            val parsedRaw = extrinsicGson.parseArbitraryObject(payload.message.raw!!)

            val wrapped = mapOf(
                "data" to payload.message.data,
                "raw" to parsedRaw
            )

            extrinsicGson.toJson(wrapped)
        }.getOrElse {
            extrinsicGson.toJson(payload.message)
        }
    }

    private fun originAccountId() = payload.originAddress.asEthereumAddress().toAccountId().value

    private fun mapMetamaskChainToUi(metamaskChain: MetamaskChain): ChainUi {
        return ChainUi(
            id = metamaskChain.chainId,
            name = metamaskChain.chainName,
            gradient = mapGradientToUi(Chain.Gradient.Default),
            icon = metamaskChain.iconUrls?.firstOrNull()
        )
    }

    private suspend fun EthereumApi.formTransaction(basedOn: MetamaskTransaction): RawTransaction {
        return formTransaction(
            fromAddress = basedOn.from,
            toAddress = basedOn.to,
            data = basedOn.data,
            value = basedOn.value?.removeHexPrefix()?.toBigIntegerOrNull(16)
        )
    }

    private fun createTokenFrom(metamaskChain: MetamaskChain): Token {
        val currency = metamaskChain.nativeCurrency

        return Token(
            dollarRate = null,
            recentRateChange = null,
            configuration = Chain.Asset(
                iconUrl = metamaskChain.iconUrls?.firstOrNull(),
                id = 0,
                priceId = null,
                chainId = metamaskChain.chainId,
                symbol = currency.symbol,
                precision = currency.decimals,
                buyProviders = emptyMap(),
                staking = Chain.Asset.StakingType.UNSUPPORTED,
                type = Chain.Asset.Type.Native,
                name = currency.name
            )
        )
    }
}

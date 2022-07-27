package io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.sign

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.EmptyValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_dapp_impl.data.network.ethereum.EthereumApi
import io.novafoundation.nova.feature_dapp_impl.data.network.ethereum.EthereumApiFactory
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.MetamaskInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.ConfirmDAppOperationValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.ConfirmDAppOperationValidationSystem
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DAppSignInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.convertingToAmount
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.failedSigningIfNotCancelled
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskSendTransactionRequest
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskSendTransactionRequest.Payload
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskTransaction
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.PersonalSignMessage
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.TypedMessage
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAddress
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toAccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw
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
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val ethereumApiFactory: EthereumApiFactory,
    private val signerProvider: SignerProvider
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
        signerProvider = signerProvider
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
    private val accountRepository: AccountRepository,
    private val signerProvider: SignerProvider,
) : DAppSignInteractor {

    private val mostRecentFormedTx = singleReplaySharedFlow<RawTransaction>()

    val payload = request.payload

    private val ethereumApi by lazy {
        val nodeUrl = request.payload.chain.rpcUrls.first()

        ethereumApiFactory.create(nodeUrl)
    }

    override val validationSystem = when (payload) {
        is Payload.SendTx -> transactionValidationSystem()
        else -> EmptyValidationSystem()
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

        tx.fee()
    }

    override suspend fun performOperation(): DAppSignCommunicator.Response? = withContext(Dispatchers.Default) {
        runCatching {
            when (payload) {
                is Payload.SendTx -> sendTx(payload.transaction)
                is Payload.SignTypedMessage -> signTypedMessage(payload.message)
                is Payload.PersonalSign -> personalSign(payload.message)
            }
        }.getOrElse { error ->
            Log.e(LOG_TAG, "Failed to sign tx from Metamask", error)

            error.failedSigningIfNotCancelled(request.id)
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

    private fun transactionValidationSystem(): ConfirmDAppOperationValidationSystem {
        return ValidationSystem {
            sufficientBalance(
                fee = { payload ->
                    payload.convertingToAmount { mostRecentFormedTx.first().fee() }
                },
                amount = { payload ->
                    payload.convertingToAmount { mostRecentFormedTx.first().value ?: BigInteger.ZERO }
                },
                available = { validationPayload ->
                    validationPayload.convertingToAmount { ethereumApi.getAccountBalance(payload.originAddress) }
                },
                error = { NotEnoughBalanceToPayFees }
            )
        }
    }

    private suspend fun sendTx(basedOn: MetamaskTransaction): DAppSignCommunicator.Response.Sent {
        val tx = ethereumApi.formTransaction(basedOn)

        val originAccountId = originAccountId()

        val chainId = request.payload.chain.chainId.removeHexPrefix().toLong(16)

        val txHash = ethereumApi.sendTransaction(tx, resolveSigner(), originAccountId, chainId)

        return DAppSignCommunicator.Response.Sent(request.id, txHash)
    }

    private suspend fun signTypedMessage(message: TypedMessage): DAppSignCommunicator.Response.Signed {
        val signature = signMessage(message.data.fromHex())

        return DAppSignCommunicator.Response.Signed(request.id, signature)
    }

    private suspend fun personalSign(message: PersonalSignMessage): DAppSignCommunicator.Response.Signed {
        val personalSignMessage = message.data.fromHex().asEthereumPersonalSignMessage()
        val payload = SignerPayloadRaw(personalSignMessage, originAccountId(), skipMessageHashing = true)

        val signature = resolveSigner().signRaw(payload).asHexString()

        return DAppSignCommunicator.Response.Signed(request.id, signature)
    }

    private suspend fun signMessage(message: ByteArray): String {
        val signerPayload = SignerPayloadRaw(message, originAccountId(), skipMessageHashing = true)

        return resolveSigner().signRaw(signerPayload).asHexString()
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

    private fun mapMetamaskChainToUi(metamaskChain: MetamaskChain): ChainUi {
        return ChainUi(
            id = metamaskChain.chainId,
            name = metamaskChain.chainName,
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

    private suspend fun resolveSigner(): Signer {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        return signerProvider.signerFor(metaAccount)
    }

    private fun RawTransaction.fee() = gasLimit * gasPrice

    private fun originAccountId() = payload.originAddress.asEthereumAddress().toAccountId().value
}

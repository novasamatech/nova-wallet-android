package io.novafoundation.nova.feature_external_sign_impl.domain.sign.evm

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
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
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.sign.asEthereumPersonalSignMessage
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.failedSigningIfNotCancelled
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChain
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChainSource
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload.SendTx
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTransaction
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmPersonalSignMessage
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTypedMessage
import io.novafoundation.nova.feature_external_sign_impl.data.evmApi.EvmApi
import io.novafoundation.nova.feature_external_sign_impl.data.evmApi.EvmApiFactory
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ConfirmDAppOperationValidationFailure
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ConfirmDAppOperationValidationSystem
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ExternalSignInteractor
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.convertingToAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import io.novafoundation.nova.runtime.multiNetwork.findEvmChain
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAddress
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toAccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.web3j.crypto.RawTransaction
import java.math.BigInteger

class EvmSignInteractorFactory(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val currencyRepository: CurrencyRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val evmApiFactory: EvmApiFactory,
    private val signerProvider: SignerProvider
) {

    fun create(request: ExternalSignRequest.Evm) = EvmSignInteractor(
        chainRegistry = chainRegistry,
        tokenRepository = tokenRepository,
        currencyRepository = currencyRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        request = request,
        evmApiFactory = evmApiFactory,
        accountRepository = accountRepository,
        signerProvider = signerProvider
    )
}

class EvmSignInteractor(
    private val evmApiFactory: EvmApiFactory,
    private val request: ExternalSignRequest.Evm,
    private val addressIconGenerator: AddressIconGenerator,
    private val tokenRepository: TokenRepository,
    private val currencyRepository: CurrencyRepository,
    private val chainRegistry: ChainRegistry,
    private val extrinsicGson: Gson,
    private val accountRepository: AccountRepository,
    private val signerProvider: SignerProvider,
) : ExternalSignInteractor, CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val mostRecentFormedTx = singleReplaySharedFlow<RawTransaction>()

    private val payload = request.payload

    @OptIn(DelicateCoroutinesApi::class)
    private val ethereumApi by GlobalScope.lazyAsync {
        payload.castOrNull<SendTx>()?.chainSource?.let { chainSource ->
            evmApiFactory.create(chainSource)
        }
    }

    override val validationSystem = when (payload) {
        is SendTx -> transactionValidationSystem()
        else -> EmptyValidationSystem()
    }

    override suspend fun createAccountAddressModel(): AddressModel = withContext(Dispatchers.Default) {
        val address = request.payload.originAddress
        val someEthereumChain = chainRegistry.findChain { it.isEthereumBased }!! // always have at least one ethereum chain in the app

        addressIconGenerator.createAccountAddressModel(someEthereumChain, address)
    }

    override suspend fun chainUi(): Result<ChainUi?> = withContext(Dispatchers.Default) {
        runCatching {
            if (payload is SendTx) {
                chainRegistry.findEvmChain(payload.chainSource.evmChainId)?.let(::mapChainToUi)
                    ?: payload.chainSource.fallbackChain?.let(::mapEvmChainToUi)
                    ?: throw ExternalSignInteractor.Error.UnsupportedChain(payload.chainSource.evmChainId.toString())
            } else {
                null
            }
        }
    }

    override fun commissionTokenFlow(): Flow<Token?>? {
        if (payload !is SendTx) return null

        return flow {
            val chain = chainRegistry.findEvmChain(payload.chainSource.evmChainId)

            if (chain != null) {
                emitAll(tokenRepository.observeToken(chain.utilityAsset))
            } else {
                emit(createTokenFrom(payload.chainSource.unknownChainOptions))
            }
        }
    }

    override suspend fun calculateFee(): Balance = withContext(Dispatchers.Default) {
        if (payload !is SendTx) return@withContext Balance.ZERO

        val api = ethereumApi() ?: return@withContext Balance.ZERO

        val tx = api.formTransaction(payload.transaction)
        mostRecentFormedTx.emit(tx)

        tx.fee()
    }

    override suspend fun performOperation(): ExternalSignCommunicator.Response? = withContext(Dispatchers.Default) {
        runCatching {
            when (payload) {
                is SendTx -> sendTx(payload.transaction, payload.chainSource.evmChainId)
                is EvmSignPayload.SignTypedMessage -> signTypedMessage(payload.message)
                is EvmSignPayload.PersonalSign -> personalSign(payload.message)
            }
        }.getOrElse { error ->
            Log.e(LOG_TAG, "Failed to sign evm tx", error)

            error.failedSigningIfNotCancelled(request.id)
        }
    }

    override suspend fun readableOperationContent(): String = withContext(Dispatchers.Default) {
        when (payload) {
            is SendTx -> extrinsicGson.toJson(mostRecentFormedTx.first())
            is EvmSignPayload.SignTypedMessage -> signTypedMessageReadableContent(payload)
            is EvmSignPayload.PersonalSign -> personalSignReadableContent(payload)
        }
    }

    override suspend fun shutdown() {
        ethereumApi()?.shutdown()
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
                    validationPayload.convertingToAmount { ethereumApi()!!.getAccountBalance(payload.originAddress) }
                },
                error = { _, _ -> ConfirmDAppOperationValidationFailure.NotEnoughBalanceToPayFees },
                skippable = true
            )
        }
    }

    private suspend fun sendTx(basedOn: EvmTransaction, evmChainId: Int): ExternalSignCommunicator.Response.Sent {
        val api = requireNotNull(ethereumApi())

        val tx = api.formTransaction(basedOn)

        val originAccountId = originAccountId()

        val txHash = api.sendTransaction(tx, resolveSigner(), originAccountId, evmChainId.toLong())

        return ExternalSignCommunicator.Response.Sent(request.id, txHash)
    }

    private suspend fun signTypedMessage(message: EvmTypedMessage): ExternalSignCommunicator.Response.Signed {
        val signature = signMessage(message.data.fromHex())

        return ExternalSignCommunicator.Response.Signed(request.id, signature)
    }

    private suspend fun personalSign(message: EvmPersonalSignMessage): ExternalSignCommunicator.Response.Signed {
        val personalSignMessage = message.data.fromHex().asEthereumPersonalSignMessage()
        val payload = SignerPayloadRaw(personalSignMessage, originAccountId(), skipMessageHashing = true)

        val signature = resolveSigner().signRaw(payload).asHexString()

        return ExternalSignCommunicator.Response.Signed(request.id, signature)
    }

    private suspend fun signMessage(message: ByteArray): String {
        val signerPayload = SignerPayloadRaw(message, originAccountId(), skipMessageHashing = true)

        return resolveSigner().signRaw(signerPayload).asHexString()
    }

    private fun personalSignReadableContent(payload: EvmSignPayload.PersonalSign): String {
        val data = payload.message.data

        return runCatching { data.fromHex().decodeToString(throwOnInvalidSequence = true) }
            .getOrDefault(data)
    }

    private fun signTypedMessageReadableContent(payload: EvmSignPayload.SignTypedMessage): String {
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

    private fun mapEvmChainToUi(metamaskChain: EvmChain): ChainUi {
        return ChainUi(
            id = metamaskChain.chainId,
            name = metamaskChain.chainName,
            icon = metamaskChain.iconUrl
        )
    }

    private suspend fun EvmApi.formTransaction(basedOn: EvmTransaction): RawTransaction {
        return formTransaction(
            fromAddress = basedOn.from,
            toAddress = basedOn.to,
            data = basedOn.data,
            value = basedOn.value?.removeHexPrefix()?.toBigIntegerOrNull(16)
        )
    }

    private suspend fun createTokenFrom(unknownChainOptions: EvmChainSource.UnknownChainOptions): Token? {
        if (unknownChainOptions !is EvmChainSource.UnknownChainOptions.WithFallBack) return null

        val evmChain = unknownChainOptions.evmChain

        val currency = currencyRepository.getSelectedCurrency()
        val chainCurrency = evmChain.nativeCurrency

        return Token(
            rate = null,
            recentRateChange = null,
            configuration = Chain.Asset(
                iconUrl = evmChain.iconUrl,
                id = 0,
                priceId = null,
                chainId = evmChain.chainId,
                symbol = chainCurrency.symbol,
                precision = chainCurrency.decimals,
                buyProviders = emptyMap(),
                staking = Chain.Asset.StakingType.UNSUPPORTED,
                type = Chain.Asset.Type.EvmNative,
                name = chainCurrency.name,
                source = Chain.Asset.Source.ERC20,
                enabled = true
            ),
            currency = currency
        )
    }

    private suspend fun resolveSigner(): Signer {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        return signerProvider.signerFor(metaAccount)
    }

    private fun RawTransaction.fee() = gasLimit * gasPrice

    private fun originAccountId() = payload.originAddress.asEthereumAddress().toAccountId().value

    private val EvmChainSource.fallbackChain: EvmChain?
        get() = when (val options = unknownChainOptions) {

            EvmChainSource.UnknownChainOptions.MustBeKnown -> null

            is EvmChainSource.UnknownChainOptions.WithFallBack -> options.evmChain
        }
}

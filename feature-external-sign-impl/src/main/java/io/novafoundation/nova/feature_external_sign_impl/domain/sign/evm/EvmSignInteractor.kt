package io.novafoundation.nova.feature_external_sign_impl.domain.sign.evm

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.decodeEvmQuantity
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.model.EvmFee
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.failedSigningIfNotCancelled
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignWallet
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChain
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChainSource
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmPersonalSignMessage
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload.ConfirmTx
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload.PersonalSign
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload.SignTypedMessage
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTransaction
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTypedMessage
import io.novafoundation.nova.feature_external_sign_impl.data.evmApi.EvmApi
import io.novafoundation.nova.feature_external_sign_impl.data.evmApi.EvmApiFactory
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.BaseExternalSignInteractor
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ConfirmDAppOperationValidationFailure
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ConfirmDAppOperationValidationSystem
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ExternalSignInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.validation.checkForFeeChanges
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import io.novafoundation.nova.runtime.multiNetwork.findEvmChain
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionDecoder

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

    fun create(request: ExternalSignRequest.Evm, wallet: ExternalSignWallet) = EvmSignInteractor(
        chainRegistry = chainRegistry,
        tokenRepository = tokenRepository,
        currencyRepository = currencyRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        request = request,
        evmApiFactory = evmApiFactory,
        accountRepository = accountRepository,
        signerProvider = signerProvider,
        wallet = wallet
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
    private val signerProvider: SignerProvider,
    accountRepository: AccountRepository,
    wallet: ExternalSignWallet,
) : BaseExternalSignInteractor(accountRepository, wallet, signerProvider),
    CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val mostRecentFormedTx = singleReplaySharedFlow<RawTransaction>()

    private val payload = request.payload

    @OptIn(DelicateCoroutinesApi::class)
    private val ethereumApi by GlobalScope.lazyAsync {
        payload.castOrNull<ConfirmTx>()?.chainSource?.let { chainSource ->
            evmApiFactory.create(chainSource)
        }
    }

    override val validationSystem: ConfirmDAppOperationValidationSystem = ValidationSystem {
        if (payload is ConfirmTx) {
            checkForFeeChanges(
                calculateFee = { calculateFee()!! },
                currentFee = { it.fee },
                chainAsset = { it.fee!!.asset },
                error = ConfirmDAppOperationValidationFailure::FeeSpikeDetected
            )
        }
    }

    override suspend fun createAccountAddressModel(): AddressModel = withContext(Dispatchers.Default) {
        val address = request.payload.originAddress
        val someEthereumChain = chainRegistry.findChain { it.isEthereumBased }!! // always have at least one ethereum chain in the app

        addressIconGenerator.createAccountAddressModel(someEthereumChain, address)
    }

    override suspend fun chainUi(): Result<ChainUi?> = withContext(Dispatchers.Default) {
        runCatching {
            if (payload is ConfirmTx) {
                chainRegistry.findEvmChain(payload.chainSource.evmChainId)?.let(::mapChainToUi)
                    ?: payload.chainSource.fallbackChain?.let(::mapEvmChainToUi)
                    ?: throw ExternalSignInteractor.Error.UnsupportedChain(payload.chainSource.evmChainId.toString())
            } else {
                null
            }
        }
    }

    override fun utilityAssetFlow(): Flow<Chain.Asset>? {
        if (payload !is ConfirmTx) return null

        return flowOf {
            chainRegistry.findEvmChain(payload.chainSource.evmChainId)?.utilityAsset
                ?: createAssetFrom(payload.chainSource.unknownChainOptions)
        }.filterNotNull()
    }

    override suspend fun calculateFee(): Fee? = withContext(Dispatchers.Default) {
        if (payload !is ConfirmTx) return@withContext null

        resolveWalletSigner()

        val api = ethereumApi() ?: return@withContext null
        val chain = chainRegistry.findEvmChain(payload.chainSource.evmChainId)

        // Commission asset for evm
        val chainAsset = chain?.utilityAsset ?: createAssetFrom(payload.chainSource.unknownChainOptions) ?: return@withContext null

        val tx = api.formTransaction(payload.transaction, feeOverride = null)
        mostRecentFormedTx.emit(tx)

        tx.fee(chainAsset)
    }

    override suspend fun performOperation(upToDateFee: Fee?): ExternalSignCommunicator.Response? = withContext(Dispatchers.Default) {
        runCatching {
            when (payload) {
                is ConfirmTx -> confirmTx(payload.transaction, upToDateFee, payload.chainSource.evmChainId.toLong(), payload.action)
                is SignTypedMessage -> signTypedMessage(payload.message)
                is PersonalSign -> personalSign(payload.message)
            }
        }.getOrElse { error ->
            Log.e(LOG_TAG, "Failed to sign evm tx", error)

            error.failedSigningIfNotCancelled(request.id)
        }
    }

    override suspend fun readableOperationContent(): String = withContext(Dispatchers.Default) {
        when (payload) {
            is ConfirmTx -> extrinsicGson.toJson(mostRecentFormedTx.first())
            is SignTypedMessage -> signTypedMessageReadableContent(payload)
            is PersonalSign -> personalSignReadableContent(payload)
        }
    }

    override suspend fun shutdown() {
        ethereumApi()?.shutdown()
    }

    private suspend fun confirmTx(basedOn: EvmTransaction, upToDateFee: Fee?, evmChainId: Long, action: ConfirmTx.Action): ExternalSignCommunicator.Response {
        val api = requireNotNull(ethereumApi())

        val tx = api.formTransaction(basedOn, upToDateFee)

        val originAccountId = originAccountId()
        val signer = resolveWalletSigner()

        return when (action) {
            ConfirmTx.Action.SIGN -> {
                val signedTx = api.signTransaction(tx, signer, originAccountId, evmChainId)
                ExternalSignCommunicator.Response.Signed(request.id, signedTx)
            }

            ConfirmTx.Action.SEND -> {
                val txHash = api.sendTransaction(tx, signer, originAccountId, evmChainId)
                ExternalSignCommunicator.Response.Sent(request.id, txHash)
            }
        }
    }

    private suspend fun signTypedMessage(message: EvmTypedMessage): ExternalSignCommunicator.Response.Signed {
        val signature = signMessage(message.data.fromHex())

        return ExternalSignCommunicator.Response.Signed(request.id, signature)
    }

    private suspend fun personalSign(message: EvmPersonalSignMessage): ExternalSignCommunicator.Response.Signed {
        val personalSignMessage = message.data.fromHex().asEthereumPersonalSignMessage()
        val payload = SignerPayloadRaw(personalSignMessage, originAccountId(), skipMessageHashing = true)

        val signature = resolveWalletSigner().signRaw(payload).asHexString()

        return ExternalSignCommunicator.Response.Signed(request.id, signature)
    }

    private suspend fun signMessage(message: ByteArray): String {
        val signerPayload = SignerPayloadRaw(message, originAccountId(), skipMessageHashing = true)

        return resolveWalletSigner().signRaw(signerPayload).asHexString()
    }

    private fun personalSignReadableContent(payload: PersonalSign): String {
        val data = payload.message.data

        return runCatching { data.fromHex().decodeToString(throwOnInvalidSequence = true) }
            .getOrDefault(data)
    }

    private fun signTypedMessageReadableContent(payload: SignTypedMessage): String {
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

    private suspend fun EvmApi.formTransaction(basedOn: EvmTransaction, feeOverride: Fee?): RawTransaction {
        return when (basedOn) {
            is EvmTransaction.Raw -> TransactionDecoder.decode(basedOn.rawContent)

            is EvmTransaction.Struct -> {
                val evmFee = feeOverride.castOrNull<EvmFee>()

                formTransaction(
                    fromAddress = basedOn.from,
                    toAddress = basedOn.to,
                    data = basedOn.data,
                    value = basedOn.value?.decodeEvmQuantity(),
                    nonce = basedOn.nonce?.decodeEvmQuantity(),
                    gasLimit = evmFee?.gasLimit ?: basedOn.gas?.decodeEvmQuantity(),
                    gasPrice = evmFee?.gasPrice ?: basedOn.gasPrice?.decodeEvmQuantity()
                )
            }
        }
    }

    private suspend fun createTokenFrom(unknownChainOptions: EvmChainSource.UnknownChainOptions): Token? {
        if (unknownChainOptions !is EvmChainSource.UnknownChainOptions.WithFallBack) return null
        val asset = createAssetFrom(unknownChainOptions) ?: return null

        return Token(
            configuration = asset,
            coinRate = null,
            currency = currencyRepository.getSelectedCurrency()
        )
    }

    private fun createAssetFrom(unknownChainOptions: EvmChainSource.UnknownChainOptions): Chain.Asset? {
        if (unknownChainOptions !is EvmChainSource.UnknownChainOptions.WithFallBack) return null

        val evmChain = unknownChainOptions.evmChain
        val chainCurrency = evmChain.nativeCurrency

        return Chain.Asset(
            icon = null,
            id = 0,
            priceId = null,
            chainId = evmChain.chainId,
            symbol = chainCurrency.symbol,
            precision = chainCurrency.decimals,
            buyProviders = emptyMap(),
            sellProviders = emptyMap(),
            staking = emptyList(),
            type = Chain.Asset.Type.EvmNative,
            name = chainCurrency.name,
            source = Chain.Asset.Source.ERC20,
            enabled = true
        )
    }

    private fun RawTransaction.fee(chainAsset: Chain.Asset): Fee = EvmFee(
        gasLimit = gasLimit,
        gasPrice = gasPrice,
        submissionOrigin = submissionOrigin(),
        chainAsset
    )

    private fun submissionOrigin() = SubmissionOrigin.singleOrigin(originAccountId())

    private fun originAccountId() = payload.originAddress.asEthereumAddress().toAccountId().value

    private val EvmChainSource.fallbackChain: EvmChain?
        get() = when (val options = unknownChainOptions) {

            EvmChainSource.UnknownChainOptions.MustBeKnown -> null

            is EvmChainSource.UnknownChainOptions.WithFallBack -> options.evmChain
        }
}

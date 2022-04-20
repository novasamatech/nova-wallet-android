package io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.sign

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.mappers.mapGradientToUi
import io.novafoundation.nova.feature_account_api.data.secrets.getEthereumKeypair
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
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAddress
import jp.co.soramitsu.fearless_utils.extensions.toAccountId
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
        ethereumApi = ethereumApiFactory.create(request.payload.chain.rpcUrls.first()),
        accountRepository = accountRepository,
        secretStoreV2 = secretStoreV2
    )
}

class MetamaskSignInteractor(
    private val ethereumApi: EthereumApi,
    private val request: MetamaskSendTransactionRequest,
    private val metamaskInteractor: MetamaskInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val tokenRepository: TokenRepository,
    private val chainRegistry: ChainRegistry,
    private val extrinsicGson: Gson,
    private val secretStoreV2: SecretStoreV2,
    private val accountRepository: AccountRepository,
) : DAppSignInteractor {

    val mostRecentFormedTx = singleReplaySharedFlow<RawTransaction>()

    override suspend fun createAccountAddressModel(): AddressModel {
        val address = request.payload.transaction.from
        val someEthereumChain = chainRegistry.findChain { it.isEthereumBased }!! // always have at least one ethereum chain in the app

        return addressIconGenerator.createAccountAddressModel(someEthereumChain, address)
    }

    override suspend fun chainUi(): ChainUi {
        val metamaskChain = request.payload.chain

        return metamaskInteractor.tryFindChainFromEthereumChainId(metamaskChain.chainId)?.let(::mapChainToUi)
            ?: mapMetamaskChainToUi(metamaskChain)
    }

    override fun commissionTokenFlow(): Flow<Token> {
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
        val tx = ethereumApi.formTransaction()
        mostRecentFormedTx.emit(tx)

        tx.gasLimit * tx.gasPrice
    }

    override suspend fun performOperation(): DAppSignCommunicator.Response = withContext(Dispatchers.Default) {
        runCatching {
            val tx = ethereumApi.formTransaction()
            val originAccountId = request.payload.transaction.from.asEthereumAddress().toAccountId().value
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val keypair = secretStoreV2.getEthereumKeypair(metaAccount, originAccountId)
            val chainId = request.payload.chain.chainId.removeHexPrefix().toLong(16)

            val txHash = ethereumApi.sendTransaction(tx, keypair, chainId)

            DAppSignCommunicator.Response.Sent(request.id, txHash)
        }.getOrElse {
            Log.e(LOG_TAG, "Failed to sign tx from Metamask", it)

            DAppSignCommunicator.Response.SigningFailed(request.id)
        }
    }

    override suspend fun readableOperationContent(): String = withContext(Dispatchers.Default) {
        extrinsicGson.toJson(mostRecentFormedTx.first())
    }

    override fun shutdown() {
        return ethereumApi.shutdown()
    }

    private fun mapMetamaskChainToUi(metamaskChain: MetamaskChain): ChainUi {
        return ChainUi(
            id = metamaskChain.chainId,
            name = metamaskChain.chainName,
            gradient = mapGradientToUi(Chain.Gradient.Default),
            icon = metamaskChain.iconUrls?.firstOrNull()
        )
    }

    private suspend fun EthereumApi.formTransaction(): RawTransaction {
        val txPayload = request.payload.transaction

        return formTransaction(
            fromAddress = txPayload.from,
            toAddress = txPayload.to,
            data = txPayload.data,
            value = txPayload.value
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

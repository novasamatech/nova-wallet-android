package io.novafoundation.nova.feature_gift_impl.domain

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.secrets.v2.keypair
import io.novafoundation.nova.common.data.secrets.v2.publicKey
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.normalizeSeed
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.EvmFee
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.repository.CreateSecretsRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.account.common.forChain
import io.novafoundation.nova.feature_gift_impl.data.GiftSecretsRepository
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.models.CreateGiftModel
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.BaseAssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.emptyAccountIdKey
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.encrypt.seed.SeedCreator
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val GIFT_SEED_SIZE_BYTES = 10

typealias GiftId = Long

interface CreateGiftInteractor {
    fun validationSystemFor(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): AssetTransfersValidationSystem

    suspend fun getFee(
        model: CreateGiftModel,
        transferAllToCreateGift: Boolean,
        coroutineScope: CoroutineScope
    ): GiftFee

    suspend fun getExistentialDeposit(chainAsset: Chain.Asset): BigDecimal

    suspend fun createAndSaveGift(
        giftModel: CreateGiftModel,
        transfer: WeightedAssetTransfer,
        fee: SubmissionFee,
        coroutineScope: CoroutineScope
    ): Result<GiftId>
}

class RealCreateGiftInteractor(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val createSecretsRepository: CreateSecretsRepository,
    private val chainRegistry: ChainRegistry,
    private val encryptionDefaults: EncryptionDefaults,
    private val giftSecretsRepository: GiftSecretsRepository,
    private val giftsRepository: GiftsRepository,
    private val sendUseCase: SendUseCase,
) : CreateGiftInteractor {

    override fun validationSystemFor(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): AssetTransfersValidationSystem {
        return getAssetTransfers(chainAsset)
            .getValidationSystem(coroutineScope)
    }

    override suspend fun getFee(
        model: CreateGiftModel,
        transferAllToCreateGift: Boolean,
        coroutineScope: CoroutineScope
    ): GiftFee = withContext(Dispatchers.Default) {
        val claimGiftFee = getSubmissionFee(
            model = model,
            transferMax = true,
            giftAccountId = model.chain.emptyAccountIdKey(),
            coroutineScope = coroutineScope
        ).doubleFeeForEvm()
        val claimFeeAmount = model.chainAsset.amountFromPlanks(claimGiftFee.amount)

        val createGiftFee = getSubmissionFee(
            model = model.copy(amount = model.amount + claimFeeAmount),
            transferMax = transferAllToCreateGift,
            giftAccountId = model.chain.emptyAccountIdKey(),
            coroutineScope = coroutineScope
        )
        GiftFee(
            createGiftFee = createGiftFee,
            claimGiftFee = claimGiftFee
        )
    }

    override suspend fun getExistentialDeposit(chainAsset: Chain.Asset): BigDecimal {
        return assetSourceRegistry.existentialDeposit(chainAsset)
    }

    override suspend fun createAndSaveGift(
        giftModel: CreateGiftModel,
        transfer: WeightedAssetTransfer,
        fee: SubmissionFee,
        coroutineScope: CoroutineScope
    ): Result<GiftId> {
        val giftAccountId = createAndStoreRandomGiftAccount(giftModel.chain.id)
        val gitAddress = giftModel.chain.addressOf(giftAccountId)
        val giftTransfer = transfer.copy(recipient = gitAddress)
        return sendUseCase.performOnChainTransfer(giftTransfer, fee, coroutineScope)
            .map {
                Log.d(LOG_TAG, "Gift was created successfully. Address in ${giftModel.chain.name}: $gitAddress")

                giftsRepository.saveNewGift(
                    accountIdKey = giftAccountId,
                    amount = giftModel.chainAsset.planksFromAmount(giftModel.amount),
                    fullChainAssetId = giftModel.chainAsset.fullId
                )
            }
    }

    private suspend fun getSubmissionFee(
        model: CreateGiftModel,
        transferMax: Boolean,
        giftAccountId: AccountIdKey,
        coroutineScope: CoroutineScope
    ): SubmissionFee {
        return withContext(Dispatchers.Default) {
            val transfer = model.mapToAssetTransfer(giftAccountId, transferMax)
            getAssetTransfers(model.chainAsset).calculateFee(transfer, coroutineScope = coroutineScope)
        }
    }

    private fun getAssetTransfers(chainAsset: Chain.Asset) = assetSourceRegistry.sourceFor(chainAsset).transfers

    private fun CreateGiftModel.mapToAssetTransfer(giftAccountId: AccountIdKey, transferMax: Boolean) = BaseAssetTransfer(
        sender = senderMetaAccount,
        recipient = chain.addressOf(giftAccountId),
        originChain = chain,
        originChainAsset = chainAsset,
        destinationChain = chain,
        destinationChainAsset = chainAsset,
        feePaymentCurrency = FeePaymentCurrency.Asset(chainAsset),
        amount = amount,
        transferringMaxAmount = transferMax
    )

    private suspend fun createAndStoreRandomGiftAccount(chainId: String): AccountIdKey {
        val chain = chainRegistry.getChain(chainId)
        val encryption = encryptionDefaults.forChain(chain)

        val seed = SeedCreator.randomSeed(sizeBytes = GIFT_SEED_SIZE_BYTES)
        val giftSecrets = createSecretsRepository.createSecretsWithSeed(
            seed = seed.normalizeSeed(),
            cryptoType = encryption.cryptoType,
            derivationPath = encryption.derivationPath,
            isEthereum = chain.isEthereumBased
        )

        val accountId = chain.accountIdOf(giftSecrets.keypair.publicKey)

        giftSecretsRepository.putGiftAccountSecrets(accountId, giftSecrets)

        return accountId.intoKey()
    }

    private fun SubmissionFee.doubleFeeForEvm(): SubmissionFee {
        return when (this) {
            is EvmFee -> copy(gasLimit = gasLimit * 2.toBigInteger())
            else -> this
        }
    }
}

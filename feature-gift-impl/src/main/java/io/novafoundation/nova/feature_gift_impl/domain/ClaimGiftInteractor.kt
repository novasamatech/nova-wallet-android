package io.novafoundation.nova.feature_gift_impl.domain

import android.util.Log
import io.novafoundation.nova.common.data.secrets.v2.keypair
import io.novafoundation.nova.common.data.secrets.v2.publicKey
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_gift_impl.data.GiftSecretsRepository
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.models.ClaimableGift
import io.novafoundation.nova.feature_gift_impl.domain.models.CreateGiftModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

interface ClaimGiftInteractor {

    suspend fun getClaimableGift(secret: ByteArray, chainId: String, assetId: Int): ClaimableGift

    suspend fun observeGiftAmount(giftAccount: AccountId, chainId: String, assetId: Int): BigInteger

    suspend fun claimGift(claimableGift: ClaimableGift): Result<Unit>
}

class RealClaimGiftInteractor(
    private val giftsRepository: GiftsRepository,
    private val giftSecretsRepository: GiftSecretsRepository,
    private val giftSecretsUseCase: GiftSecretsUseCase,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val sendUseCase: SendUseCase,
) : ClaimGiftInteractor {

    override suspend fun getClaimableGift(secret: ByteArray, chainId: String, assetId: Int): ClaimableGift {
        val chain = chainRegistry.getChain(chainId)
        val giftSecrets = giftSecretsUseCase.createRandomGiftSecrets(chain)
        return ClaimableGift(
            accountId = chain.accountIdOf(giftSecrets.keypair.publicKey),
            chainId = chainId,
            assetId = assetId,
            secrets = giftSecrets
        )
    }

    override suspend fun observeGiftAmount(
        giftAccount: AccountId,
        chainId: String,
        assetId: Int
    ): BigInteger {
        val chain = chainRegistry.getChain(chainId)
        val chainAsset = chainRegistry.asset(chainId, assetId)
        val assetBalanceSource = assetSourceRegistry.sourceFor(chainAsset).balance

        return assetBalanceSource.queryAccountBalance(chain, chainAsset, giftAccount).free
    }

    override suspend fun claimGift(claimableGift: ClaimableGift): Result<Unit> {

    }


    override suspend fun createAndSaveGift(
        giftModel: CreateGiftModel,
        transfer: WeightedAssetTransfer,
        fee: SubmissionFee,
        coroutineScope: CoroutineScope
    ): Result<GiftId> {
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
}

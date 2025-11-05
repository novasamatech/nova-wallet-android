package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.keypair
import io.novafoundation.nova.common.data.secrets.v2.publicKey
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.common.utils.finally
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.domain.interfaces.CreateGiftMetaAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_gift_impl.domain.models.ClaimableGift
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftAmountWithFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.BaseAssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.asWeighted
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

interface ClaimGiftInteractor {

    suspend fun getClaimableGift(secret: ByteArray, chainId: String, assetId: Int): ClaimableGift

    suspend fun getGiftAmountWithFee(
        claimableGift: ClaimableGift,
        giftMetaAccount: MetaAccount,
        coroutineScope: CoroutineScope
    ): GiftAmountWithFee

    suspend fun createTempMetaAccount(claimableGift: ClaimableGift): MetaAccount

    suspend fun isGiftAlreadyClaimed(claimableGift: ClaimableGift): Boolean

    suspend fun claimGift(
        claimableGift: ClaimableGift,
        giftAmountWithFee: GiftAmountWithFee,
        giftMetaAccount: MetaAccount,
        coroutineScope: CoroutineScope
    ): Result<Unit>
}

class RealClaimGiftInteractor(
    private val giftSecretsUseCase: GiftSecretsUseCase,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val sendUseCase: SendUseCase,
    private val createGiftMetaAccountUseCase: CreateGiftMetaAccountUseCase,
    private val secretStoreV2: SecretStoreV2,
    private val selectedAccountUseCase: SelectedAccountUseCase
) : ClaimGiftInteractor {

    override suspend fun getClaimableGift(secret: ByteArray, chainId: String, assetId: Int): ClaimableGift {
        val chain = chainRegistry.getChain(chainId)
        val giftSecrets = giftSecretsUseCase.createGiftSecrets(chain, secret)
        return ClaimableGift(
            accountId = chain.accountIdOf(giftSecrets.keypair.publicKey),
            chain = chain,
            chainAsset = chain.assetsById.getValue(assetId),
            secrets = giftSecrets
        )
    }

    override suspend fun createTempMetaAccount(claimableGift: ClaimableGift): MetaAccount {
        return createGiftMetaAccountUseCase.createTemporaryGiftMetaAccount(claimableGift.chain, claimableGift.secrets)
    }

    override suspend fun isGiftAlreadyClaimed(claimableGift: ClaimableGift): Boolean {
        val giftBalance = getGiftAccountBalance(claimableGift)
        return giftBalance.isZero
    }

    override suspend fun getGiftAmountWithFee(
        claimableGift: ClaimableGift,
        giftMetaAccount: MetaAccount,
        coroutineScope: CoroutineScope
    ): GiftAmountWithFee {
        val accountBalance = claimableGift.chainAsset.amountFromPlanks(getGiftAccountBalance(claimableGift))
        val transferModel = createTransfer(claimableGift, giftMetaAccount, accountBalance)
        val claimFee = assetSourceRegistry.sourceFor(claimableGift.chainAsset)
            .transfers
            .calculateFee(transferModel, coroutineScope)

        return GiftAmountWithFee(accountBalance - claimFee.decimalAmount, claimFee)
    }

    override suspend fun claimGift(
        claimableGift: ClaimableGift,
        giftAmountWithFee: GiftAmountWithFee,
        giftMetaAccount: MetaAccount,
        coroutineScope: CoroutineScope
    ): Result<Unit> {
        // Put secrets for temporary meta account in storage but for this operation only since signer logic requires secrets in secret storage
        secretStoreV2.putChainAccountSecrets(giftMetaAccount.id, claimableGift.accountId, claimableGift.secrets)

        return claimGiftInternal(claimableGift, giftMetaAccount, giftAmountWithFee, coroutineScope)
            .finally {
                // Remove secrets for temporary meta account from storage after claim or failure
                secretStoreV2.clearChainAccountsSecrets(giftMetaAccount.id, listOf(claimableGift.accountId))
            }
    }

    private suspend fun claimGiftInternal(
        giftModel: ClaimableGift,
        giftMetaAccount: MetaAccount,
        giftAmountWithFee: GiftAmountWithFee,
        coroutineScope: CoroutineScope
    ): Result<Unit> {
        val originFee = OriginFee(submissionFee = giftAmountWithFee.fee, deliveryFee = null)
        val giftTransfer = createTransfer(giftModel, giftMetaAccount, amount = giftAmountWithFee.amount).asWeighted(originFee)
        return sendUseCase.performOnChainTransfer(giftTransfer, originFee.submissionFee, coroutineScope)
            .coerceToUnit()
    }

    private suspend fun getGiftAccountBalance(claimableGift: ClaimableGift): BigInteger {
        val assetBalanceSource = assetSourceRegistry.sourceFor(claimableGift.chainAsset).balance

        return assetBalanceSource.queryAccountBalance(claimableGift.chain, claimableGift.chainAsset, claimableGift.accountId).transferable
    }

    private suspend fun createTransfer(
        giftModel: ClaimableGift,
        giftMetaAccount: MetaAccount,
        amount: BigDecimal
    ): BaseAssetTransfer {
        val selectedAccount = selectedAccountUseCase.getSelectedMetaAccount()
        return BaseAssetTransfer(
            sender = giftMetaAccount,
            recipient = selectedAccount.requireAddressIn(giftModel.chain),
            originChain = giftModel.chain,
            originChainAsset = giftModel.chainAsset,
            destinationChain = giftModel.chain,
            destinationChainAsset = giftModel.chainAsset,
            feePaymentCurrency = FeePaymentCurrency.Asset(giftModel.chainAsset),
            amount = amount,
            transferringMaxAmount = true
        )
    }
}

package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.keypair
import io.novafoundation.nova.common.data.secrets.v2.publicKey
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.common.utils.finally
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency.Asset.Companion.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.CreateGiftMetaAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.isControllableWallet
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_gift_impl.domain.models.ClaimableGift
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftAmountWithFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.BaseAssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.asWeighted
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal
import java.math.BigInteger

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
        giftRecipient: MetaAccount,
        coroutineScope: CoroutineScope
    ): Result<Unit>

    suspend fun getMetaAccount(metaId: Long): MetaAccount

    suspend fun getMetaAccountToClaimGift(): MetaAccount
}

class RealClaimGiftInteractor(
    private val giftSecretsUseCase: GiftSecretsUseCase,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val sendUseCase: SendUseCase,
    private val createGiftMetaAccountUseCase: CreateGiftMetaAccountUseCase,
    private val secretStoreV2: SecretStoreV2,
    private val accountRepository: AccountRepository
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
        val transferModel = createTransfer(
            claimableGift,
            giftMetaAccount,
            recipientAccountId = claimableGift.chain.emptyAccountId(),
            accountBalance
        )
        val claimFee = assetSourceRegistry.sourceFor(claimableGift.chainAsset)
            .transfers
            .calculateFee(transferModel, coroutineScope)

        return GiftAmountWithFee(accountBalance - claimFee.decimalAmount, claimFee)
    }

    override suspend fun claimGift(
        claimableGift: ClaimableGift,
        giftAmountWithFee: GiftAmountWithFee,
        giftMetaAccount: MetaAccount,
        giftRecipient: MetaAccount,
        coroutineScope: CoroutineScope
    ): Result<Unit> {
        // Put secrets for temporary meta account in storage but for this operation only since signer logic requires secrets in secret storage
        secretStoreV2.putChainAccountSecrets(giftMetaAccount.id, claimableGift.accountId, claimableGift.secrets)

        return claimGiftInternal(
            giftModel = claimableGift,
            giftMetaAccount = giftMetaAccount,
            giftRecipient = giftRecipient,
            giftAmountWithFee = giftAmountWithFee,
            coroutineScope = coroutineScope
        )
            .finally {
                // Remove secrets for temporary meta account from storage after claim or failure
                secretStoreV2.clearChainAccountsSecrets(giftMetaAccount.id, listOf(claimableGift.accountId))
            }
    }

    override suspend fun getMetaAccount(metaId: Long): MetaAccount {
        return accountRepository.getMetaAccount(metaId)
    }

    override suspend fun getMetaAccountToClaimGift(): MetaAccount {
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        if (selectedMetaAccount.type.isControllableWallet()) return selectedMetaAccount

        val firstControllableWallet = accountRepository.getActiveMetaAccounts()
            .firstOrNull { it.type.isControllableWallet() }

        return firstControllableWallet ?: selectedMetaAccount
    }

    private suspend fun claimGiftInternal(
        giftModel: ClaimableGift,
        giftMetaAccount: MetaAccount,
        giftRecipient: MetaAccount,
        giftAmountWithFee: GiftAmountWithFee,
        coroutineScope: CoroutineScope
    ): Result<Unit> {
        val originFee = OriginFee(submissionFee = giftAmountWithFee.fee, deliveryFee = null)
        val giftTransfer = createTransfer(
            giftModel,
            giftMetaAccount,
            recipientAccountId = giftRecipient.requireAccountIdIn(giftModel.chain),
            amount = giftAmountWithFee.amount
        ).asWeighted(originFee)

        return sendUseCase.performOnChainTransferAndAwaitExecution(giftTransfer, originFee.submissionFee, coroutineScope)
            .coerceToUnit()
    }

    private suspend fun getGiftAccountBalance(claimableGift: ClaimableGift): BigInteger {
        val assetBalanceSource = assetSourceRegistry.sourceFor(claimableGift.chainAsset).balance

        return assetBalanceSource.queryAccountBalance(
            claimableGift.chain,
            claimableGift.chainAsset,
            claimableGift.accountId
        ).total
    }

    private fun createTransfer(
        giftModel: ClaimableGift,
        giftMetaAccount: MetaAccount,
        recipientAccountId: AccountId,
        amount: BigDecimal
    ): BaseAssetTransfer {
        return BaseAssetTransfer(
            sender = giftMetaAccount,
            recipient = giftModel.chain.addressOf(recipientAccountId),
            originChain = giftModel.chain,
            originChainAsset = giftModel.chainAsset,
            destinationChain = giftModel.chain,
            destinationChainAsset = giftModel.chainAsset,
            feePaymentCurrency = giftModel.chainAsset.toFeePaymentCurrency(),
            amount = amount,
            transferringMaxAmount = true
        )
    }
}

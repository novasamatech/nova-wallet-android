package io.novafoundation.nova.feature_assets.domain.send

import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.model.amountByRequestedAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_assets.domain.send.model.TransferFeeModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.isCrossChain
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.senderAccountId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFeeModel
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.implementations.transferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginDecimalFee
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.domain.model.RecipientSearchResult
import io.novafoundation.nova.feature_wallet_api.domain.model.networkFeePart
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendInteractor(
    private val walletRepository: WalletRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val crossChainWeigher: CrossChainWeigher,
    private val crossChainTransactor: CrossChainTransactor,
    private val crossChainTransfersRepository: CrossChainTransfersRepository,
    private val parachainInfoRepository: ParachainInfoRepository,
) {

    // TODO wallet
    suspend fun getRecipients(query: String, chainId: ChainId): RecipientSearchResult {
//        val metaAccount = accountRepository.getSelectedMetaAccount()
//        val chain = chainRegistry.getChain(chainId)
//        val accountId = metaAccount.accountIdIn(chain)!!
//
//        val contacts = walletRepository.getContacts(accountId, chain, query)
//        val myAccounts = accountRepository.getMyAccounts(query, chain.id)
//
//        return withContext(Dispatchers.Default) {
//            val contactsWithoutMyAccounts = contacts - myAccounts.map { it.address }
//            val myAddressesWithoutCurrent = myAccounts - metaAccount
//
//            RecipientSearchResult(
//                myAddressesWithoutCurrent.toList().map { mapAccountToWalletAccount(chain, it) },
//                contactsWithoutMyAccounts.toList()
//            )
//        }

        return RecipientSearchResult(
            myAccounts = emptyList(),
            contacts = emptyList()
        )
    }

    suspend fun getFee(amount: Balance, transfer: AssetTransfer): TransferFeeModel = withContext(Dispatchers.Default) {
        if (transfer.isCrossChain) {
            val config = crossChainTransfersRepository.getConfiguration().configurationFor(transfer)!!

            val originFee = crossChainTransactor.estimateOriginFee(config, transfer)
            val crossChainFeeModel = crossChainWeigher.estimateFee(amount, config)

            val deliveryPartFee = getDeliveryFee(crossChainFeeModel.senderPart, transfer.senderAccountId())
            val originFeeWithSenderPart = OriginFee(originFee, deliveryPartFee, transfer.commissionAssetToken.configuration)

            TransferFeeModel(originFeeWithSenderPart, crossChainFeeModel.toSubstrateFee(transfer))
        } else {
            val originFee = getAssetTransfers(transfer).calculateFee(transfer)
            TransferFeeModel(
                OriginFee(originFee, null, transfer.commissionAssetToken.configuration),
                null
            )
        }
    }

    suspend fun performTransfer(
        transfer: WeightedAssetTransfer,
        originFee: OriginDecimalFee,
        crossChainFee: Fee?,
    ): Result<*> = withContext(Dispatchers.Default) {
        if (transfer.isCrossChain) {
            val config = crossChainTransfersRepository.getConfiguration().configurationFor(transfer)!!

            crossChainTransactor.performTransfer(config, transfer, crossChainFee!!.amountByRequestedAccount)
        } else {
            val networkFee = originFee.networkFeePart()

            getAssetTransfers(transfer).performTransfer(transfer)
                .onSuccess { submission ->
                    // Insert used fee regardless of who paid it
                    walletRepository.insertPendingTransfer(submission.hash, transfer, networkFee.networkFeeDecimalAmount)
                }
        }
    }

    fun validationSystemFor(transfer: AssetTransfer) = if (transfer.isCrossChain) {
        crossChainTransactor.validationSystem
    } else {
        assetSourceRegistry.sourceFor(transfer.originChainAsset).transfers.validationSystem
    }

    suspend fun areTransfersEnabled(asset: Chain.Asset) = assetSourceRegistry.sourceFor(asset).transfers.areTransfersEnabled(asset)

    private fun getAssetTransfers(transfer: AssetTransfer) = assetSourceRegistry.sourceFor(transfer.originChainAsset).transfers

    private suspend fun CrossChainTransfersConfiguration.configurationFor(transfer: AssetTransfer) = transferConfiguration(
        originChain = transfer.originChain,
        originAsset = transfer.originChainAsset,
        destinationChain = transfer.destinationChain,
        destinationParaId = parachainInfoRepository.paraId(transfer.destinationChain.id)
    )

    private fun getDeliveryFee(amount: Balance, accountId: AccountId): Fee {
        return SubstrateFee(
            amount = amount,
            submissionOrigin = SubmissionOrigin.singleOrigin(accountId)
        )
    }

    private fun CrossChainFeeModel.toSubstrateFee(transfer: AssetTransfer) = SubstrateFee(
        amount = holdingPart,
        submissionOrigin = SubmissionOrigin.singleOrigin(transfer.sender.requireAccountIdIn(transfer.originChain))
    )
}

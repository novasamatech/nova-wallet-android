package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.common.data.model.CursorPage
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.OperationsPageChange
import io.novafoundation.nova.feature_wallet_api.domain.model.RecipientSearchResult
import io.novafoundation.nova.feature_wallet_api.domain.model.Transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.math.BigDecimal

class NotValidTransferStatus(val status: TransferValidityStatus) : Exception()

interface WalletInteractor {

    fun assetsFlow(): Flow<List<Asset>>

    suspend fun syncAssetsRates(): Result<Unit>

    fun assetFlow(chainId: ChainId, chainAssetId: Int): Flow<Asset>

    suspend fun getCurrentAsset(chainId: ChainId, chainAssetId: Int): Asset

    fun operationsFirstPageFlow(chainId: ChainId, chainAssetId: Int): Flow<OperationsPageChange>

    suspend fun syncOperationsFirstPage(
        chainId: ChainId,
        chainAssetId: Int,
        pageSize: Int,
        filters: Set<TransactionFilter>,
    ): Result<*>

    suspend fun getOperations(
        chainId: ChainId,
        chainAssetId: Int,
        pageSize: Int,
        cursor: String?,
        filters: Set<TransactionFilter>
    ): Result<CursorPage<Operation>>

    suspend fun getRecipients(query: String, chainId: ChainId): RecipientSearchResult

    suspend fun validateSendAddress(chainId: ChainId, address: String): Boolean

    suspend fun isAddressFromPhishingList(address: String): Boolean

    suspend fun getTransferFee(transfer: Transfer): Fee

    suspend fun performTransfer(
        transfer: Transfer,
        fee: BigDecimal,
        maxAllowedLevel: TransferValidityLevel = TransferValidityLevel.Ok
    ): Result<Unit>

    suspend fun checkTransferValidityStatus(transfer: Transfer): Result<TransferValidityStatus>

    suspend fun getQrCodeSharingString(chainId: ChainId): String

    suspend fun createFileInTempStorageAndRetrieveAsset(
        chainId: ChainId,
        chainAssetId: Int,
        fileName: String
    ): Result<Pair<File, Asset>>

    suspend fun getRecipientFromQrCodeContent(content: String): Result<String>
}

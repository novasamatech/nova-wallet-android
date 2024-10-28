package io.novafoundation.nova.feature_assets.domain.receive

import android.graphics.Bitmap
import android.net.Uri
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.data.repository.AssetsIconModeService
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.utils.write
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val QR_FILE_NAME = "share-qr-address.png"

class ReceiveInteractor(
    private val fileProvider: FileProvider,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val assetsIconModeService: AssetsIconModeService
) {

    suspend fun getQrCodeSharingString(chainId: ChainId): String = withContext(Dispatchers.Default) {
        val chain = chainRegistry.getChain(chainId)
        val account = accountRepository.getSelectedMetaAccount()

        accountRepository.createQrAccountContent(chain, account)
    }

    fun getAssetIconMode(): AssetIconMode = assetsIconModeService.getIconMode()

    suspend fun generateTempQrFile(qrCode: Bitmap): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileProvider.generateTempFile(fixedName = QR_FILE_NAME)
            file.write(qrCode)

            fileProvider.uriOf(file)
        }
    }
}

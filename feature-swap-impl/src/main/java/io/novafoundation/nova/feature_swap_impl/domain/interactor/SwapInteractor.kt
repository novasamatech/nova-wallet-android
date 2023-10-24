package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.fullId
import kotlinx.coroutines.CoroutineScope

class SwapInteractor(
    private val swapService: SwapService,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository
) {

    suspend fun availableAssets(coroutineScope: CoroutineScope): List<Asset> {
        val chainsWithAssets = swapService.assetsAvailableForSwap(coroutineScope)
        val metaAccount = accountRepository.getSelectedMetaAccount()
        return walletRepository.getSupportedAssets(metaAccount.id)
            .filter {
                val fullId = it.token.configuration.fullId
                chainsWithAssets.contains(fullId)
            }
    }

    suspend fun quote(quoteArgs: SwapQuoteArgs): Result<SwapQuote> {
        return swapService.quote(quoteArgs)
    }

    suspend fun estimateFee(executeArgs: SwapExecuteArgs): SwapFee {
        return swapService.estimateFee(executeArgs)
    }
}

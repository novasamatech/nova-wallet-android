package io.novafoundation.nova.feature_multisig_operations.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization

interface MultisigOperationsFeatureDependencies {

    val extrinsicSplitter: ExtrinsicSplitter

    val accountRepository: AccountRepository

    val extrinsicService: ExtrinsicService

    val resourceManager: ResourceManager

    val multisigPendingOperationsService: MultisigPendingOperationsService

    val feeLoaderMixinFactory: FeeLoaderMixinV2.Factory

    val imageLoader: ImageLoader

    val externalActions: ExternalActions.Presentation

    val validationExecutor: ValidationExecutor

    val selectedAccountUseCase: SelectedAccountUseCase

    val walletUiUseCase: WalletUiUseCase

    val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper

    val arbitraryAssetUseCase: ArbitraryAssetUseCase

    val edValidationFactory: EnoughTotalToStayAboveEDValidationFactory

    val assetSourceRegistry: AssetSourceRegistry

    @ExtrinsicSerialization
    fun extrinsicGson(): Gson
}

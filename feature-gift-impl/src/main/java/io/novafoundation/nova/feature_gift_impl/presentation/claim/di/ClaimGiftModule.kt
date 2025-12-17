package io.novafoundation.nova.feature_gift_impl.presentation.claim.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletMixin
import io.novafoundation.nova.feature_gift_impl.domain.ClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftPayload
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftViewModel
import io.novafoundation.nova.feature_gift_impl.presentation.common.UnpackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.common.claim.ClaimGiftMixinFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ClaimGiftModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): ClaimGiftViewModel {
        return ViewModelProvider(fragment, factory).get(ClaimGiftViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ClaimGiftViewModel::class)
    fun provideViewModel(
        router: GiftRouter,
        payload: ClaimGiftPayload,
        claimGiftInteractor: ClaimGiftInteractor,
        chainRegistry: ChainRegistry,
        unpackingGiftAnimationFactory: UnpackingGiftAnimationFactory,
        assetIconProvider: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        resourceManager: ResourceManager,
        walletUiUseCase: WalletUiUseCase,
        claimGiftMixinFactory: ClaimGiftMixinFactory,
        accountInteractor: AccountInteractor,
        selectSingleWalletMixin: SelectSingleWalletMixin.Factory,
    ): ViewModel {
        return ClaimGiftViewModel(
            router = router,
            payload = payload,
            claimGiftInteractor = claimGiftInteractor,
            chainRegistry = chainRegistry,
            unpackingGiftAnimationFactory = unpackingGiftAnimationFactory,
            assetIconProvider = assetIconProvider,
            tokenFormatter = tokenFormatter,
            resourceManager = resourceManager,
            walletUiUseCase = walletUiUseCase,
            claimGiftMixinFactory = claimGiftMixinFactory,
            selectSingleWalletMixin = selectSingleWalletMixin,
            accountInteractor = accountInteractor
        )
    }
}

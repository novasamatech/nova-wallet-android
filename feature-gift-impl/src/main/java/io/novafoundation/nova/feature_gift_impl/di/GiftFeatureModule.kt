package io.novafoundation.nova.feature_gift_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.feature_account_api.data.repository.CreateSecretsRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.CreateGiftMetaAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_gift_impl.data.GiftSecretsRepository
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.data.RealGiftSecretsRepository
import io.novafoundation.nova.feature_gift_impl.data.RealGiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.ClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.GiftsInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealGiftsInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealCreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.GiftSecretsUseCase
import io.novafoundation.nova.feature_gift_impl.domain.RealClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealShareGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.ShareGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.amount.GiftMinAmountProviderFactory
import io.novafoundation.nova.feature_gift_impl.presentation.common.PackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.common.UnpackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.common.claim.ClaimGiftMixinFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module()
class GiftFeatureModule {

    @Provides
    @FeatureScope
    fun providesGiftsRepository(
        giftsDao: GiftsDao
    ): GiftsRepository {
        return RealGiftsRepository(giftsDao)
    }

    @Provides
    @FeatureScope
    fun providesGiftsInteractor(
        repository: GiftsRepository,
        assetSourceRegistry: AssetSourceRegistry,
        chainRegistry: ChainRegistry,
        selectedAccountUseCase: SelectedAccountUseCase
    ): GiftsInteractor {
        return RealGiftsInteractor(repository, assetSourceRegistry, chainRegistry, selectedAccountUseCase)
    }

    @Provides
    @FeatureScope
    fun providesGiftSecretsRepository(encryptedPreferences: EncryptedPreferences): GiftSecretsRepository {
        return RealGiftSecretsRepository(encryptedPreferences)
    }

    @Provides
    @FeatureScope
    fun provideSelectGiftAmountInteractor(
        assetSourceRegistry: AssetSourceRegistry,
        chainRegistry: ChainRegistry,
        giftSecretsRepository: GiftSecretsRepository,
        giftsRepository: GiftsRepository,
        sendUseCase: SendUseCase,
        giftSecretsUseCase: GiftSecretsUseCase
    ): CreateGiftInteractor {
        return RealCreateGiftInteractor(
            assetSourceRegistry,
            chainRegistry,
            giftSecretsRepository,
            giftsRepository,
            sendUseCase,
            giftSecretsUseCase
        )
    }

    @Provides
    @FeatureScope
    fun provideGiftMinAmountProviderFactory(
        createGiftInteractor: CreateGiftInteractor
    ): GiftMinAmountProviderFactory {
        return GiftMinAmountProviderFactory(createGiftInteractor)
    }

    @Provides
    @FeatureScope
    fun providesShareGiftInteractor(
        giftsRepository: GiftsRepository,
        giftSecretsRepository: GiftSecretsRepository
    ): ShareGiftInteractor {
        return RealShareGiftInteractor(giftsRepository, giftSecretsRepository)
    }

    @Provides
    @FeatureScope
    fun providesPackingGiftAnimationFactory(): PackingGiftAnimationFactory {
        return PackingGiftAnimationFactory()
    }

    @Provides
    @FeatureScope
    fun providesUnpackingGiftAnimationFactory(): UnpackingGiftAnimationFactory {
        return UnpackingGiftAnimationFactory()
    }

    @Provides
    @FeatureScope
    fun provideClaimGiftInteractor(
        giftSecretsUseCase: GiftSecretsUseCase,
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
        sendUseCase: SendUseCase,
        createGiftMetaAccountUseCase: CreateGiftMetaAccountUseCase,
        secretStoreV2: SecretStoreV2,
        accountRepository: AccountRepository
    ): ClaimGiftInteractor {
        return RealClaimGiftInteractor(
            giftSecretsUseCase,
            chainRegistry,
            assetSourceRegistry,
            sendUseCase,
            createGiftMetaAccountUseCase,
            secretStoreV2,
            accountRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideGiftSecretsUseCase(
        createSecretsRepository: CreateSecretsRepository,
        encryptionDefaults: EncryptionDefaults
    ): GiftSecretsUseCase {
        return GiftSecretsUseCase(
            createSecretsRepository,
            encryptionDefaults
        )
    }

    @Provides
    @FeatureScope
    fun provideClaimGiftMixinFactory(claimGiftInteractor: ClaimGiftInteractor) = ClaimGiftMixinFactory(claimGiftInteractor)
}

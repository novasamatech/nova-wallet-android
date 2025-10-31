package io.novafoundation.nova.feature_gift_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.feature_account_api.data.repository.CreateSecretsRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_gift_impl.data.GiftSecretsRepository
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.data.RealGiftSecretsRepository
import io.novafoundation.nova.feature_gift_impl.data.RealGiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.GiftsInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealGiftsInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealCreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealShareGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.ShareGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.amount.GiftMinAmountProviderFactory
import io.novafoundation.nova.feature_gift_impl.presentation.common.PackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.common.UnpackingGiftAnimationFactory
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
    fun providesGiftsInteractor(repository: GiftsRepository): GiftsInteractor {
        return RealGiftsInteractor(repository)
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
        createSecretsRepository: CreateSecretsRepository,
        chainRegistry: ChainRegistry,
        encryptionDefaults: EncryptionDefaults,
        giftSecretsRepository: GiftSecretsRepository,
        giftsRepository: GiftsRepository,
        sendUseCase: SendUseCase,
    ): CreateGiftInteractor {
        return RealCreateGiftInteractor(
            assetSourceRegistry,
            createSecretsRepository,
            chainRegistry,
            encryptionDefaults,
            giftSecretsRepository,
            giftsRepository,
            sendUseCase
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
        secretStoreV2: SecretStoreV2
    ): ShareGiftInteractor {
        return RealShareGiftInteractor(giftsRepository, secretStoreV2)
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
}

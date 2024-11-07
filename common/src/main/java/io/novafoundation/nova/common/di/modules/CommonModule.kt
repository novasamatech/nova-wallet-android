package io.novafoundation.nova.common.di.modules

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Vibrator
import coil.ImageLoader
import coil.decode.SvgDecoder
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.CachingAddressIconGenerator
import io.novafoundation.nova.common.address.StatelessAddressIconGenerator
import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.data.FileProviderImpl
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.data.RealGoogleApiAvailabilityProvider
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.RealComputationalCache
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.data.repository.RealAssetsIconModeRepository
import io.novafoundation.nova.common.data.repository.RealAssetsViewModeRepository
import io.novafoundation.nova.common.data.repository.RealBannerVisibilityRepository
import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1
import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1Impl
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.PreferencesImpl
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferencesImpl
import io.novafoundation.nova.common.data.storage.encrypt.EncryptionUtil
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.domain.interactor.RealAssetViewModeInteractor
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.interfaces.InternalFileSystemCache
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableProvider
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.condition.ConditionMixinFactory
import io.novafoundation.nova.common.mixin.condition.RealConditionMixinFactory
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.mixin.impl.CustomDialogProvider
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.RealAssetIconProvider
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.common.resources.OSAppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.ResourceManagerImpl
import io.novafoundation.nova.common.sequrity.RealSafeModeService
import io.novafoundation.nova.common.sequrity.RealTwoFactorVerificationService
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationExecutor
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationCommunicator
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationExecutor
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.utils.multiResult.RealPartialRetriableMixinFactory
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.utils.sequrity.RealAutomaticInteractionGate
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.bottomSheet.action.RealActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.description.RealDescriptionBottomSheetLauncher
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.common.view.input.chooser.RealListChooserMixinFactory
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.common.view.input.selector.RealListSelectorMixinFactory
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.icon.IconGenerator
import java.security.SecureRandom
import java.util.Random
import javax.inject.Qualifier

const val SHARED_PREFERENCES_FILE = "fearless_prefs"

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Caching

@Module(includes = [ParallaxCardModule::class])
class CommonModule {

    @Provides
    @ApplicationScope
    fun provideGoogleApiAvailabilityProvider(
        context: Context
    ): GoogleApiAvailabilityProvider {
        return RealGoogleApiAvailabilityProvider(context)
    }

    @Provides
    @ApplicationScope
    fun provideComputationalCache(): ComputationalCache = RealComputationalCache()

    @Provides
    @ApplicationScope
    fun imageLoader(context: Context) = ImageLoader.Builder(context)
        .componentRegistry {
            add(SvgDecoder(context))
        }
        .build()

    @Provides
    @ApplicationScope
    fun provideResourceManager(contextManager: ContextManager): ResourceManager {
        return ResourceManagerImpl(contextManager)
    }

    @Provides
    @ApplicationScope
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
    }

    @Provides
    @ApplicationScope
    fun providePreferences(sharedPreferences: SharedPreferences): Preferences {
        return PreferencesImpl(sharedPreferences)
    }

    @Provides
    @ApplicationScope
    fun provideInteractionGate(): AutomaticInteractionGate = RealAutomaticInteractionGate()

    @Provides
    @ApplicationScope
    fun provideBackgroundAccessObserver(
        preferences: Preferences,
        automaticInteractionGate: AutomaticInteractionGate
    ): BackgroundAccessObserver {
        return BackgroundAccessObserver(preferences, automaticInteractionGate)
    }

    @Provides
    @ApplicationScope
    fun provideEncryptionUtil(context: Context): EncryptionUtil {
        return EncryptionUtil(context)
    }

    @Provides
    @ApplicationScope
    fun provideEncryptedPreferences(
        preferences: Preferences,
        encryptionUtil: EncryptionUtil,
    ): EncryptedPreferences {
        return EncryptedPreferencesImpl(preferences, encryptionUtil)
    }

    @Provides
    @ApplicationScope
    fun provideSigner(): Signer {
        return Signer
    }

    @Provides
    @ApplicationScope
    fun provideIconGenerator(): IconGenerator {
        return IconGenerator()
    }

    @Provides
    @ApplicationScope
    fun provideClipboardManager(context: Context): ClipboardManager {
        return ClipboardManager(context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
    }

    @Provides
    @ApplicationScope
    fun provideDeviceVibrator(context: Context): DeviceVibrator {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        return DeviceVibrator(vibrator)
    }

    @Provides
    @ApplicationScope
    fun provideLanguagesHolder(): LanguagesHolder {
        return LanguagesHolder()
    }

    @Provides
    @ApplicationScope
    fun provideAddressModelCreator(
        resourceManager: ResourceManager,
        iconGenerator: IconGenerator,
    ): AddressIconGenerator = StatelessAddressIconGenerator(iconGenerator, resourceManager)

    @Provides
    @Caching
    fun provideCachingAddressModelCreator(
        delegate: AddressIconGenerator,
    ): AddressIconGenerator = CachingAddressIconGenerator(delegate)

    @Provides
    @ApplicationScope
    fun provideQrCodeGenerator(): QrCodeGenerator {
        return QrCodeGenerator(Color.BLACK, Color.WHITE)
    }

    @Provides
    @ApplicationScope
    fun provideFileProvider(contextManager: ContextManager): FileProvider {
        return FileProviderImpl(contextManager.getApplicationContext())
    }

    @Provides
    @ApplicationScope
    fun provideRandom(): Random = SecureRandom()

    @Provides
    @ApplicationScope
    fun provideContentResolver(
        context: Context,
    ): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @ApplicationScope
    fun provideValidationExecutor(): ValidationExecutor {
        return ValidationExecutor()
    }

    @Provides
    @ApplicationScope
    fun provideSecretStoreV1(
        encryptedPreferences: EncryptedPreferences,
    ): SecretStoreV1 = SecretStoreV1Impl(encryptedPreferences)

    @Provides
    @ApplicationScope
    fun provideSecretStoreV2(
        encryptedPreferences: EncryptedPreferences,
    ) = SecretStoreV2(encryptedPreferences)

    @Provides
    @ApplicationScope
    fun provideCustomDialogDisplayer(): CustomDialogDisplayer.Presentation = CustomDialogProvider()

    @Provides
    @ApplicationScope
    fun provideAppVersionsProvider(context: Context): AppVersionProvider {
        return OSAppVersionProvider(context)
    }

    @Provides
    @ApplicationScope
    fun provideSystemCallExecutor(
        contextManager: ContextManager
    ): SystemCallExecutor = SystemCallExecutor(contextManager)

    @Provides
    @ApplicationScope
    fun actionAwaitableMixinFactory(): ActionAwaitableMixin.Factory = ActionAwaitableProvider

    @Provides
    @ApplicationScope
    fun resourcesHintsMixinFactory(
        resourceManager: ResourceManager,
    ) = ResourcesHintsMixinFactory(resourceManager)

    @Provides
    @ApplicationScope
    fun provideFileCache(fileProvider: FileProvider): FileCache = InternalFileSystemCache(fileProvider)

    @Provides
    @ApplicationScope
    fun providePermissionAskerFactory(
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ) = PermissionsAskerFactory(actionAwaitableMixinFactory)

    @Provides
    @ApplicationScope
    fun provideEthereumAddressFormat() = EthereumAddressFormat()

    @Provides
    @ApplicationScope
    fun provideListChooserMixinFactory(
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ): ListChooserMixin.Factory = RealListChooserMixinFactory(actionAwaitableMixinFactory)

    @Provides
    @ApplicationScope
    fun provideSafeModeService(
        contextManager: ContextManager,
        preferences: Preferences
    ): SafeModeService {
        return RealSafeModeService(contextManager, preferences)
    }

    @Provides
    @ApplicationScope
    fun providePartialRetriableMixinFactory(
        resourceManager: ResourceManager
    ): PartialRetriableMixin.Factory = RealPartialRetriableMixinFactory(resourceManager)

    @Provides
    @ApplicationScope
    fun provideTwoFactorVerificationExecutor(
        twoFactorVerificationExecutor: PinCodeTwoFactorVerificationCommunicator
    ): TwoFactorVerificationExecutor = PinCodeTwoFactorVerificationExecutor(twoFactorVerificationExecutor)

    @Provides
    @ApplicationScope
    fun provideTwoFactorVerificationService(
        preferences: Preferences,
        twoFactorVerificationExecutor: TwoFactorVerificationExecutor
    ): TwoFactorVerificationService = RealTwoFactorVerificationService(preferences, twoFactorVerificationExecutor)

    @Provides
    @ApplicationScope
    fun provideBannerVisibilityRepository(
        preferences: Preferences
    ): BannerVisibilityRepository = RealBannerVisibilityRepository(preferences)

    @Provides
    @ApplicationScope
    fun provideDescriptionBottomSheetLauncher(): DescriptionBottomSheetLauncher = RealDescriptionBottomSheetLauncher()

    @Provides
    @ApplicationScope
    fun provideProgressDialogMixinFactory(): ProgressDialogMixinFactory = ProgressDialogMixinFactory()

    @Provides
    @ApplicationScope
    fun provideActionBottomSheetLauncher(): ActionBottomSheetLauncherFactory = RealActionBottomSheetLauncherFactory()

    @Provides
    @ApplicationScope
    fun provideListSelectorMixinFactory(): ListSelectorMixin.Factory = RealListSelectorMixinFactory()

    @Provides
    @ApplicationScope
    fun provideConditionMixinFactory(resourceManager: ResourceManager): ConditionMixinFactory {
        return RealConditionMixinFactory(resourceManager)
    }

    @Provides
    @ApplicationScope
    fun provideCoinGeckoLinkParser(): CoinGeckoLinkParser {
        return CoinGeckoLinkParser()
    }

    @Provides
    @ApplicationScope
    fun provideAssetsViewModeRepository(preferences: Preferences): AssetsViewModeRepository = RealAssetsViewModeRepository(preferences)

    @Provides
    @ApplicationScope
    fun provideAssetViewModeInteractor(repository: AssetsViewModeRepository): AssetViewModeInteractor {
        return RealAssetViewModeInteractor(repository)
    }

    @Provides
    @ApplicationScope
    fun provideAssetsIconModeRepository(preferences: Preferences): AssetsIconModeRepository = RealAssetsIconModeRepository(preferences)

    @Provides
    @ApplicationScope
    fun provideAssetIconProvider(repository: AssetsIconModeRepository): AssetIconProvider {
        return RealAssetIconProvider(
            repository,
            BuildConfig.ASSET_COLORED_ICON_URL,
            BuildConfig.ASSET_WHITE_ICON_URL,
        )
    }
}

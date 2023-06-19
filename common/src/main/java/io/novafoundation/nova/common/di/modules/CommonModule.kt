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
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.CachingAddressIconGenerator
import io.novafoundation.nova.common.address.StatelessAddressIconGenerator
import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.data.FileProviderImpl
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.RealComputationalCache
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1
import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1Impl
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.PreferencesImpl
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferencesImpl
import io.novafoundation.nova.common.data.storage.encrypt.EncryptionUtil
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.interfaces.InternalFileSystemCache
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableProvider
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.mixin.impl.CustomDialogProvider
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
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.utils.multiResult.RealPartialRetriableMixinFactory
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.RealAutomaticInteractionGate
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.common.view.input.chooser.RealListChooserMixinFactory
import jp.co.soramitsu.fearless_utils.encrypt.Signer
import jp.co.soramitsu.fearless_utils.icon.IconGenerator
import java.security.SecureRandom
import java.util.Random
import javax.inject.Qualifier

const val SHARED_PREFERENCES_FILE = "fearless_prefs"

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Caching

@Module
class CommonModule {

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
    fun provideDefaultPagedKeysRetriever(): BulkRetriever {
        return BulkRetriever()
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
    fun provideRootScope() = RootScope()
}

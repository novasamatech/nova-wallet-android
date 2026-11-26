package io.novafoundation.nova.common.di.modules

import android.content.Context
import android.os.SystemClock
import com.google.gson.Gson
import com.neovisionaries.ws.client.WebSocketFactory
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.common.data.network.AndroidLogger
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.network.NovaUtilsMirror
import io.novafoundation.nova.common.data.network.NovaUtilsMirrorCoolDown
import io.novafoundation.nova.common.data.network.TimeHeaderInterceptor
import io.novafoundation.nova.common.data.network.rpc.SocketSingleRequestExecutor
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.mixin.api.NetworkStateMixin
import io.novafoundation.nova.common.mixin.impl.NetworkStateProvider
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.bluetooth.RealBluetoothManager
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.location.RealLocationManager
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novasama.substrate_sdk_android.wsrpc.recovery.Reconnector
import io.novasama.substrate_sdk_android.wsrpc.request.RequestExecutor
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import io.novafoundation.nova.common.data.network.DebugHttpLoggingInterceptor
import io.novafoundation.nova.common.data.storage.DebugSettings
import java.io.File
import java.util.concurrent.TimeUnit

private const val HTTP_CACHE = "http_cache"
private const val CACHE_SIZE = 50L * 1024L * 1024L // 50 MiB
private const val TIMEOUT_SECONDS = 20L

// One deadline for a whole mirror attempt, from the first address to the last byte of the body, so that a dead mirror
// delays the GitHub fallback by this much at most
private const val NOVA_UTILS_MIRROR_ATTEMPT_DEADLINE_SECONDS = 10L

// Largest mirror answer that is held in memory. The largest nova-utils file is about 0.5 MiB
private const val NOVA_UTILS_MIRROR_MAX_BODY_BYTES = 8L * 1024L * 1024L

// How long the mirror is skipped after a mirror-wide failure. Doubles while it keeps failing, up to the maximum
private const val NOVA_UTILS_MIRROR_PAUSE_SECONDS = 60L
private const val NOVA_UTILS_MIRROR_MAX_PAUSE_MINUTES = 30L

private const val SOCKET_CONNECTION_TIMEOUT = 5_000

@Module
class NetworkModule {

    @Provides
    @ApplicationScope
    fun provideAppLinksProvider(): AppLinksProvider {
        return AppLinksProvider(
            termsUrl = BuildConfig.TERMS_URL,
            privacyUrl = BuildConfig.PRIVACY_URL,
            payoutsLearnMore = BuildConfig.PAYOUTS_LEARN_MORE,
            twitterAccountTemplate = BuildConfig.TWITTER_ACCOUNT_TEMPLATE,
            setControllerLearnMore = BuildConfig.SET_CONTROLLER_LEARN_MORE,
            setControllerDeprecatedLeanMore = BuildConfig.SET_CONTROLLER_DEPRECATED_LEARN_MORE,
            recommendedValidatorsLearnMore = BuildConfig.RECOMMENDED_VALIDATORS_LEARN_MORE,
            paritySignerTroubleShooting = BuildConfig.PARITY_SIGNER_TROUBLESHOOTING,
            polkadotVaultTroubleShooting = BuildConfig.POLKADOT_VAULT_TROUBLESHOOTING,
            ledgerConnectionGuide = BuildConfig.LEDGER_CONNECTION_GUIDE,
            telegram = BuildConfig.TELEGRAM_URL,
            twitter = BuildConfig.TWITTER_URL,
            rateApp = BuildConfig.RATE_URL,
            website = BuildConfig.WEBSITE_URL,
            wikiBase = BuildConfig.NOVA_WALLET_WIKI_BASE,
            wikiProxy = BuildConfig.NOVA_WALLET_WIKI_PROXY,
            integrateNetwork = BuildConfig.NOVA_WALLET_WIKI_INTEGRATE_NETWORK,
            github = BuildConfig.GITHUB_URL,
            email = BuildConfig.EMAIL,
            youtube = BuildConfig.YOUTUBE_URL,
            storeUrl = BuildConfig.APP_UPDATE_SOURCE_LINK,
            ledgerMigrationArticle = BuildConfig.LEDGER_MIGRATION_ARTICLE,
            novaCardWidgetUrl = BuildConfig.NOVA_CARD_WIDGET_URL,
            unifiedAddressArticle = BuildConfig.UNIFIED_ADDRESS_ARTICLE,
            multisigsWikiUrl = BuildConfig.MULTISIGS_WIKI_URL,
            giftsWikiUrl = BuildConfig.GIFTS_WIKI_URL,
            watchOnlyScamWikiUrl = BuildConfig.WATCH_ONLY_SCAM_WIKI
        )
    }

    @Provides
    @ApplicationScope
    fun provideNovaUtilsMirror(logger: Logger): NovaUtilsMirror {
        val coolDown = NovaUtilsMirrorCoolDown(
            initialMillis = TimeUnit.SECONDS.toMillis(NOVA_UTILS_MIRROR_PAUSE_SECONDS),
            maxMillis = TimeUnit.MINUTES.toMillis(NOVA_UTILS_MIRROR_MAX_PAUSE_MINUTES),
            clock = SystemClock::elapsedRealtime
        )

        return NovaUtilsMirror(
            mirrorBaseUrl = BuildConfig.NOVA_UTILS_MIRROR_URL.toHttpUrl(),
            maxAttemptMillis = TimeUnit.SECONDS.toMillis(NOVA_UTILS_MIRROR_ATTEMPT_DEADLINE_SECONDS),
            maxBodyBytes = NOVA_UTILS_MIRROR_MAX_BODY_BYTES,
            coolDown = coolDown,
            logger = logger
        )
    }

    @Provides
    @ApplicationScope
    fun provideOkHttpClient(
        context: Context,
        novaUtilsMirror: NovaUtilsMirror,
        loggingInterceptor: DebugHttpLoggingInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cache(Cache(File(context.cacheDir, HTTP_CACHE), CACHE_SIZE))
            .retryOnConnectionFailure(true)
            .addInterceptor(TimeHeaderInterceptor())

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor)
        }

        // Last, on the finished client: the mirror attempt runs on that client, see NovaUtilsMirror.addTo
        return novaUtilsMirror.addTo(builder.build())
    }

    @Provides
    @ApplicationScope
    fun provideLogger(settings: DebugSettings): Logger = AndroidLogger(debug = BuildConfig.DEBUG, settings = settings)

    @Provides
    @ApplicationScope
    fun provideApiCreator(
        okHttpClient: OkHttpClient
    ): NetworkApiCreator {
        return NetworkApiCreator(okHttpClient, "https://placeholder.com")
    }

    @Provides
    @ApplicationScope
    fun httpExceptionHandler(
        resourceManager: ResourceManager
    ): HttpExceptionHandler = HttpExceptionHandler(resourceManager)

    @Provides
    @ApplicationScope
    fun provideSocketFactory() = WebSocketFactory().apply {
        connectionTimeout = SOCKET_CONNECTION_TIMEOUT
    }

    @Provides
    fun provideReconnector() = Reconnector()

    @Provides
    fun provideRequestExecutor() = RequestExecutor()

    @Provides
    fun provideSocketService(
        mapper: Gson,
        socketFactory: WebSocketFactory,
        logger: Logger,
        reconnector: Reconnector,
        requestExecutor: RequestExecutor
    ): SocketService = SocketService(mapper, logger, socketFactory, reconnector, requestExecutor)

    @Provides
    @ApplicationScope
    fun provideSocketSingleRequestExecutor(
        mapper: Gson,
        logger: Logger,
        socketFactory: WebSocketFactory,
        resourceManager: ResourceManager
    ) = SocketSingleRequestExecutor(mapper, logger, socketFactory, resourceManager)

    @Provides
    fun provideNetworkStateMixin(): NetworkStateMixin = NetworkStateProvider()

    @Provides
    @ApplicationScope
    fun provideJsonMapper() = Gson()

    @Provides
    @ApplicationScope
    fun provideBluetoothManager(
        contextManager: ContextManager,
        systemCallExecutor: SystemCallExecutor
    ): BluetoothManager = RealBluetoothManager(contextManager, systemCallExecutor)

    @Provides
    @ApplicationScope
    fun provideLocationManager(
        contextManager: ContextManager
    ): LocationManager = RealLocationManager(contextManager)
}

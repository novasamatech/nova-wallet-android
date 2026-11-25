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
import io.novafoundation.nova.common.data.network.NovaUtilsMirrorInterceptor
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
import okhttp3.Dispatcher
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import io.novafoundation.nova.common.data.network.DebugHttpLoggingInterceptor
import io.novafoundation.nova.common.data.storage.DebugSettings
import java.io.File
import java.util.concurrent.TimeUnit

private const val HTTP_CACHE = "http_cache"
private const val CACHE_SIZE = 50L * 1024L * 1024L // 50 MiB
private const val TIMEOUT_SECONDS = 20L

private const val NOVA_UTILS_MIRROR_CACHE = "nova_utils_mirror_cache"

// The whole mirror attempt - every address of the host, the headers and the body. A healthy mirror answers
// well within it, and callers with their own deadline (30 s for the Bittensor config) still reach GitHub
private const val NOVA_UTILS_MIRROR_ATTEMPT_DEADLINE_SECONDS = 5L

// How long the mirror is skipped after it could not be reached, so a dead mirror costs one deadline per window
private const val NOVA_UTILS_MIRROR_FAILURE_COOL_DOWN_SECONDS = 60L

// Mirror answers are held in memory; nova-utils files are a few hundred KiB. Larger ones are read from GitHub
private const val NOVA_UTILS_MIRROR_MAX_BODY_SIZE = 4L * 1024L * 1024L // 4 MiB

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
    fun provideNovaUtilsMirrorInterceptor(
        context: Context,
        logger: Logger
    ): NovaUtilsMirrorInterceptor {
        return NovaUtilsMirrorInterceptor(
            mirrorBaseUrl = BuildConfig.NOVA_UTILS_MIRROR_URL.toHttpUrl(),
            mirrorCallFactory = createNovaUtilsMirrorClient(context),
            attemptDeadlineMillis = TimeUnit.SECONDS.toMillis(NOVA_UTILS_MIRROR_ATTEMPT_DEADLINE_SECONDS),
            failureCoolDownMillis = TimeUnit.SECONDS.toMillis(NOVA_UTILS_MIRROR_FAILURE_COOL_DOWN_SECONDS),
            maxBodySize = NOVA_UTILS_MIRROR_MAX_BODY_SIZE,
            elapsedRealtimeMillis = SystemClock::elapsedRealtime,
            logger = logger
        )
    }

    /**
     * The client of the mirror attempts made by [NovaUtilsMirrorInterceptor] for the shared client and for the
     * image loader's client. It has its own HTTP cache: an answer the interceptor returns never passes
     * the cache of the client that owns the original request.
     */
    private fun createNovaUtilsMirrorClient(context: Context): OkHttpClient {
        // The clients that own the original requests already limit their concurrency; queueing an attempt here
        // as well would spend its deadline
        val dispatcher = Dispatcher()
        dispatcher.maxRequestsPerHost = dispatcher.maxRequests

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .cache(Cache(File(context.cacheDir, NOVA_UTILS_MIRROR_CACHE), CACHE_SIZE))
            // The mirror serves files directly; a redirect is not its answer and is never followed
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    @Provides
    @ApplicationScope
    fun provideOkHttpClient(
        context: Context,
        novaUtilsMirrorInterceptor: NovaUtilsMirrorInterceptor,
        loggingInterceptor: DebugHttpLoggingInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cache(Cache(File(context.cacheDir, HTTP_CACHE), CACHE_SIZE))
            .retryOnConnectionFailure(true)
            .addInterceptor(TimeHeaderInterceptor())
            .addInterceptor(novaUtilsMirrorInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
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

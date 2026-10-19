package io.novafoundation.nova.infrastructure.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.utils.IntegrityService
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.RealInfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.AttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.ExactJsonContentTypeInterceptor
import io.novafoundation.nova.infrastructure.attestation.IntegrityTokenSource
import io.novafoundation.nova.infrastructure.attestation.RealAttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.profile3.AesGcmProfile3TokenCipher
import io.novafoundation.nova.infrastructure.attestation.profile3.AndroidProfile3TokenClock
import io.novafoundation.nova.infrastructure.attestation.profile3.AndroidProfile3TokenEncryptionKey
import io.novafoundation.nova.infrastructure.attestation.profile3.EncryptedProfile3TokenStore
import io.novafoundation.nova.infrastructure.attestation.profile3.PreferencesProfile3IdentityStore
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3AccessTokenProvider
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3Api
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3IdentityStore
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3Interceptor
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3NetworkAdmission
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3RegistrationService
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3TokenCipher
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3TokenClock
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3TokenEncryptionKey
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3TokenRandom
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3TokenRecordFactory
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3TokenStore
import io.novafoundation.nova.infrastructure.attestation.profile3.RealProfile3AccessTokenProvider
import io.novafoundation.nova.infrastructure.attestation.profile3.SecureProfile3TokenRandom
import com.google.gson.Gson
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Authenticator
import okhttp3.CookieJar
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

/** The active Nova infrastructure transport uses profile-3 bearer authentication. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Attested

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Profile3

@Module
class AttestationModule {

    @Provides
    @ApplicationScope
    fun provideAttestationKeyPairStore(): AttestationKeyPairStore = RealAttestationKeyPairStore()

    @Provides
    @ApplicationScope
    fun provideIntegrityTokenSource(integrityService: IntegrityService): IntegrityTokenSource =
        IntegrityTokenSource { integrityService.getIntegrityToken(it) }

    @Provides
    @ApplicationScope
    @Profile3
    fun provideProfile3Preferences(context: Context): SharedPreferences =
        context.getSharedPreferences("nova.attestation.profile3", Context.MODE_PRIVATE)

    @Provides
    @ApplicationScope
    fun provideProfile3Identity(@Profile3 preferences: SharedPreferences): Profile3IdentityStore =
        PreferencesProfile3IdentityStore(preferences)

    @Provides
    @ApplicationScope
    fun provideProfile3Clock(context: Context): Profile3TokenClock = AndroidProfile3TokenClock(context)

    @Provides
    @ApplicationScope
    @Profile3
    fun provideProfile3Random(): SecureRandom = SecureRandom()

    @Provides
    @ApplicationScope
    fun provideProfile3TokenRandom(@Profile3 random: SecureRandom): Profile3TokenRandom = SecureProfile3TokenRandom(random)

    @Provides
    @ApplicationScope
    fun provideProfile3TokenFactory(clock: Profile3TokenClock, random: Profile3TokenRandom): Profile3TokenRecordFactory =
        Profile3TokenRecordFactory(clock, random)

    @Provides
    @ApplicationScope
    fun provideProfile3EncryptionKey(): Profile3TokenEncryptionKey = AndroidProfile3TokenEncryptionKey()

    @Provides
    @ApplicationScope
    fun provideProfile3Cipher(key: Profile3TokenEncryptionKey): Profile3TokenCipher = AesGcmProfile3TokenCipher(key)

    @Provides
    @ApplicationScope
    @Profile3
    fun provideProfile3TokenDirectory(context: Context): File = File(context.noBackupFilesDir, "attestation-profile3-tokens")

    @Provides
    @ApplicationScope
    fun provideProfile3TokenStore(
        @Profile3 directory: File,
        cipher: Profile3TokenCipher,
        clock: Profile3TokenClock
    ): Profile3TokenStore = EncryptedProfile3TokenStore(directory, cipher, clock)

    @Provides
    @ApplicationScope
    @Profile3
    fun provideProfile3OwnerScope(): CoroutineScope {
        val ownerJob = SupervisorJob()
        return CoroutineScope(ownerJob + Dispatchers.IO)
    }

    @Provides
    @ApplicationScope
    @Profile3
    fun provideProfile3BootstrapDispatcher(): Dispatcher = Dispatcher()

    @Provides
    @ApplicationScope
    @Profile3
    fun provideProfile3Api(okHttpClient: OkHttpClient, @Profile3 dispatcher: Dispatcher): Profile3Api =
        createBootstrapApi(okHttpClient, dispatcher, 20)

    @Provides
    @ApplicationScope
    @Named("profile3Registration")
    fun provideProfile3RegistrationApi(okHttpClient: OkHttpClient, @Profile3 dispatcher: Dispatcher): Profile3Api =
        createBootstrapApi(okHttpClient, dispatcher, 100)

    @Provides
    @ApplicationScope
    fun provideProfile3Registration(
        @Named("profile3Registration") api: Profile3Api,
        keys: AttestationKeyPairStore,
        integrityTokens: IntegrityTokenSource,
        context: Context,
        urls: InfrastructureUrls,
        identities: Profile3IdentityStore
    ): Profile3RegistrationService = Profile3RegistrationService(api, keys, integrityTokens, context.packageName, urls, identities)

    @Provides
    @ApplicationScope
    fun provideProfile3TokenProvider(
        @Profile3 api: Profile3Api,
        registration: Profile3RegistrationService,
        identities: Profile3IdentityStore,
        keys: AttestationKeyPairStore,
        tokens: Profile3TokenStore,
        tokenFactory: Profile3TokenRecordFactory,
        clock: Profile3TokenClock,
        random: Profile3TokenRandom,
        urls: InfrastructureUrls,
        @Profile3 ownerScope: CoroutineScope
    ): Profile3AccessTokenProvider = RealProfile3AccessTokenProvider(
        api, registration, identities, keys, tokens, tokenFactory, clock, random, urls, ownerScope
    )

    @Provides
    @ApplicationScope
    fun provideInfrastructureUrls(globalConfigDataSource: GlobalConfigDataSource): InfrastructureUrls =
        RealInfrastructureUrls(globalConfigDataSource)

    @Provides
    @ApplicationScope
    fun provideProfile3Interceptor(provider: Profile3AccessTokenProvider, gson: Gson): Profile3Interceptor =
        Profile3Interceptor(provider, gson)

    @Provides
    @ApplicationScope
    fun provideProfile3NetworkAdmission(provider: Profile3AccessTokenProvider): Profile3NetworkAdmission =
        Profile3NetworkAdmission(provider)

    @Provides
    @ApplicationScope
    @Attested
    fun provideAttestedNetworkApiCreator(
        okHttpClient: OkHttpClient,
        interceptor: Profile3Interceptor,
        networkAdmission: Profile3NetworkAdmission
    ): NetworkApiCreator {
        val attestedClient = okHttpClient.newBuilder()
            .addInterceptor(interceptor)
            .addNetworkInterceptor(networkAdmission)
            .followRedirects(false)
            .followSslRedirects(false)
            .authenticator(Authenticator.NONE)
            .cookieJar(CookieJar.NO_COOKIES)
            .build()
        return NetworkApiCreator(attestedClient, "https://placeholder.com")
    }

    private fun createBootstrapApi(base: OkHttpClient, dispatcher: Dispatcher, timeoutSeconds: Long): Profile3Api {
        val contentTypeInterceptor = ExactJsonContentTypeInterceptor()
        val bootstrapClient = base.newBuilder()
            .apply {
                interceptors().clear()
                networkInterceptors().clear()
            }
            .dispatcher(dispatcher)
            .addInterceptor(contentTypeInterceptor)
            .cache(null)
            .cookieJar(CookieJar.NO_COOKIES)
            .authenticator(Authenticator.NONE)
            .proxyAuthenticator(Authenticator.NONE)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
        val apiCreator = NetworkApiCreator(bootstrapClient, "https://placeholder.com")
        return apiCreator.create(Profile3Api::class.java)
    }
}

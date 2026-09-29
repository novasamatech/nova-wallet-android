package io.novafoundation.nova.infrastructure.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.RealInfrastructureUrls
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.utils.IntegrityService
import io.novafoundation.nova.infrastructure.attestation.AttestationApi
import io.novafoundation.nova.infrastructure.attestation.AttestationIdentity
import io.novafoundation.nova.infrastructure.attestation.AttestationInterceptor
import io.novafoundation.nova.infrastructure.attestation.IntegrityTokenSource
import io.novafoundation.nova.infrastructure.attestation.ExactJsonContentTypeInterceptor
import io.novafoundation.nova.infrastructure.attestation.AttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.ClientAttestationService
import io.novafoundation.nova.infrastructure.attestation.RealAttestationIdentity
import io.novafoundation.nova.infrastructure.attestation.RealAttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.RealClientAttestationService
import javax.inject.Qualifier
import okhttp3.OkHttpClient

/** Marks a [NetworkApiCreator] whose requests are signed with attestation headers. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Attested

@Module
class AttestationModule {

    @Provides
    @ApplicationScope
    fun provideAttestationIdentity(preferences: Preferences): AttestationIdentity {
        return RealAttestationIdentity(preferences)
    }

    @Provides
    @ApplicationScope
    fun provideAttestationKeyPairStore(): AttestationKeyPairStore = RealAttestationKeyPairStore()

    @Provides
    @ApplicationScope
    fun provideAttestationApi(okHttpClient: OkHttpClient): AttestationApi {
        val bootstrapClient = okHttpClient.newBuilder()
            // Registration bodies carry the integrity token and signature, which must never reach a log
            .apply { interceptors().removeAll { it.javaClass.name == HTTP_LOGGING_INTERCEPTOR } }
            .addInterceptor(ExactJsonContentTypeInterceptor())
            // A spent challenge is never replayed: a retry needs a fresh one, which only the service can get
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .build()

        return NetworkApiCreator(bootstrapClient, "https://placeholder.com").create(AttestationApi::class.java)
    }

    @Provides
    @ApplicationScope
    fun provideClientAttestationService(
        context: Context,
        api: AttestationApi,
        identity: AttestationIdentity,
        keyPairStore: AttestationKeyPairStore,
        integrityService: IntegrityService,
        infrastructureUrls: InfrastructureUrls
    ): ClientAttestationService {
        return RealClientAttestationService(
            api = api,
            identity = identity,
            keyPairStore = keyPairStore,
            integrityTokens = IntegrityTokenSource { integrityService.getIntegrityToken(it) },
            appPackage = context.packageName,
            urls = infrastructureUrls
        )
    }

    @Provides
    @ApplicationScope
    fun provideInfrastructureUrls(globalConfigDataSource: GlobalConfigDataSource): InfrastructureUrls {
        return RealInfrastructureUrls(globalConfigDataSource)
    }

    @Provides
    @ApplicationScope
    @Attested
    fun provideAttestedNetworkApiCreator(
        okHttpClient: OkHttpClient,
        attestationService: ClientAttestationService
    ): NetworkApiCreator {
        val attestedClient = okHttpClient.newBuilder()
            .addInterceptor(AttestationInterceptor(attestationService))
            // Each proof spends its challenge: OkHttp replaying a request would only earn a 401
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .build()

        return NetworkApiCreator(attestedClient, "https://placeholder.com")
    }
}

private const val HTTP_LOGGING_INTERCEPTOR = "okhttp3.logging.HttpLoggingInterceptor"

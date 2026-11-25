package io.novafoundation.nova.infrastructure

import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Where the Nova infrastructure lives.
 *
 * Read from the global remote config (`infraUrl`) rather than baked into the build, so the backend can
 * move without an app release, and dev and release builds follow their own config file.
 *
 * The one exception is the nova-utils config mirror (`NovaUtilsMirrorInterceptor`, `NOVA_UTILS_MIRROR_URL` in
 * `common/build.gradle`): the global config is read through it, so its address is a build constant that has to
 * name the same host as `infraUrl`. A build whose mirror host no longer serves reads those files from GitHub.
 */
fun interface InfrastructureUrls {

    /** The base URL, or null when the config does not name one. */
    suspend fun baseUrl(): HttpUrl?
}

/** The global config has no usable infrastructure URL - nothing can be sent until it has one. */
class InfrastructureUrlMissingException : IOException("The global config does not name an infrastructure URL")

class RealInfrastructureUrls(
    private val globalConfigDataSource: GlobalConfigDataSource
) : InfrastructureUrls {

    override suspend fun baseUrl(): HttpUrl? = globalConfigDataSource.getGlobalConfig().infraUrl?.toHttpUrlOrNull()
}

/**
 * [path] resolved against the infrastructure base URL. Fails with an IOException when there is none, so
 * callers treat it as a transient network problem rather than a verdict.
 */
suspend fun InfrastructureUrls.resolve(path: String): HttpUrl {
    val base = baseUrl() ?: throw InfrastructureUrlMissingException()

    return requireNotNull(base.resolve(path)) { "Cannot resolve $path against $base" }
}

package io.novafoundation.nova.feature_deep_linking.presentation.configuring

import android.net.Uri
import io.novafoundation.nova.common.utils.appendPathOrSkip
import io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo.BranchIOConstants
import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkingPreferences

interface LinkBuilder {

    fun setAction(action: String): LinkBuilder

    fun setEntity(entity: String): LinkBuilder

    fun setScreen(screen: String): LinkBuilder

    fun addParam(key: String, value: String): LinkBuilder

    fun build(): Uri
}

class LinkBuilderFactory(private val deepLinkingPreferences: DeepLinkingPreferences) {

    fun newLink(type: DeepLinkConfigurator.Type): LinkBuilder {
        return when (type) {
            DeepLinkConfigurator.Type.APP_LINK -> AppLinkBuilderType(deepLinkingPreferences)
            DeepLinkConfigurator.Type.DEEP_LINK -> DeepLinkBuilderType(deepLinkingPreferences)
        }
    }
}

class DeepLinkBuilderType(
    deepLinkingPreferences: DeepLinkingPreferences
) : LinkBuilder {

    private var action: String? = null
    private var entity: String? = null
    private var screen: String? = null

    private val urlBuilder = Uri.Builder()
        .scheme(deepLinkingPreferences.deepLinkScheme)
        .authority(deepLinkingPreferences.deepLinkHost)

    override fun setAction(action: String): LinkBuilder {
        this.action = action
        return this
    }

    override fun setEntity(entity: String): LinkBuilder {
        this.entity = entity
        return this
    }

    override fun setScreen(screen: String): LinkBuilder {
        this.screen = screen
        return this
    }

    override fun addParam(key: String, value: String): LinkBuilder {
        urlBuilder.appendQueryParameter(key, value)
        return this
    }

    override fun build(): Uri {
        val finalPath = Uri.Builder()
            .appendPathOrSkip(action)
            .appendPathOrSkip(entity)
            .appendPathOrSkip(screen)
            .build()
            .path

        return urlBuilder.path(finalPath).build()
    }
}

class AppLinkBuilderType(
    private val deepLinkingPreferences: DeepLinkingPreferences
) : LinkBuilder {

    private val urlBuilder = Uri.Builder()
        .scheme("https")
        .authority(deepLinkingPreferences.branchIoLinkHosts.first())

    override fun setAction(action: String): LinkBuilder {
        urlBuilder.appendQueryParameter(BranchIOConstants.ACTION_QUERY, action)
        return this
    }

    override fun setEntity(entity: String): LinkBuilder {
        urlBuilder.appendQueryParameter(BranchIOConstants.ENTITY_QUERY, entity)
        return this
    }

    override fun setScreen(screen: String): LinkBuilder {
        urlBuilder.appendQueryParameter(BranchIOConstants.SCREEN_QUERY, screen)
        return this
    }

    override fun addParam(key: String, value: String): LinkBuilder {
        urlBuilder.appendQueryParameter(key, value)
        return this
    }

    override fun build(): Uri {
        return urlBuilder.build()
    }
}

fun LinkBuilder.addParamIfNotNull(name: String, value: String?) = apply {
    value?.let { addParam(name, value) }
}

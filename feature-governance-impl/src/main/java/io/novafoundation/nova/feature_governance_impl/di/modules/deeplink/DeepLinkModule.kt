package io.novafoundation.nova.feature_governance_impl.di.modules.deeplink

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_api.di.deeplinks.GovernanceDeepLinks
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.deeplink.ReferendumDeepLinkHandler
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.deeplink.RealReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideDeepLinkConfigurator(
        linkBuilderFactory: LinkBuilderFactory
    ): ReferendumDetailsDeepLinkConfigurator {
        return RealReferendumDetailsDeepLinkConfigurator(linkBuilderFactory)
    }

    @Provides
    @FeatureScope
    fun provideReferendumDeepLinkHandler(
        router: GovernanceRouter,
        chainRegistry: ChainRegistry,
        mutableGovernanceState: MutableGovernanceState,
        automaticInteractionGate: AutomaticInteractionGate
    ): ReferendumDeepLinkHandler {
        return ReferendumDeepLinkHandler(
            router,
            chainRegistry,
            mutableGovernanceState,
            automaticInteractionGate
        )
    }

    @Provides
    @FeatureScope
    fun provideDeepLinks(referendum: ReferendumDeepLinkHandler): GovernanceDeepLinks {
        return GovernanceDeepLinks(listOf(referendum))
    }
}

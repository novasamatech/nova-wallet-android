package io.novafoundation.nova

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.util.LogUtil
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GovernancePolkassemblyTest {

    private lateinit var governanceSourceRegistry: GovernanceSourceRegistry

    private lateinit var chainRegistry: ChainRegistry

    private lateinit var chain: Chain

    @Before
    fun setup() {
        val governanceFeatureComponent = FeatureUtils.getFeature<GovernanceFeatureComponent>(
            ApplicationProvider.getApplicationContext<Context>(),
            GovernanceFeatureApi::class.java
        )

        val runtimeComponent = FeatureUtils.getFeature<RuntimeComponent>(
            ApplicationProvider.getApplicationContext<Context>(),
            RuntimeApi::class.java
        )

        governanceSourceRegistry = governanceFeatureComponent.governanceSourceRegistry
        chainRegistry = runtimeComponent.chainRegistry()

        runBlocking {
            val chainToCopy = chainRegistry.currentChains.first()[0]
            chain = chainToCopy
                .copy(
                    governance = Chain.Governance.V1,
                    externalApi = chainToCopy.externalApi?.copy(
                        governance = Chain.ExternalApi.Section(
                            Chain.ExternalApi.Section.Type.POLKASSEMBLY,
                            "https://kusama.polkassembly.io/v1/graphql"
                        )
                    )
                )
        }
    }

    @Test
    fun shouldFetchReferendaPosts() = runBlocking {
        val offChainInfoRepository = governanceSourceRegistry.sourceFor(chain.id).offChainInfo
        val previews = offChainInfoRepository.referendumPreviews(chain)
        print(previews)
    }

    @Test
    fun shouldFetchReferendaDetails() = runBlocking {
        val offChainInfoRepository = governanceSourceRegistry.sourceFor(chain.id).offChainInfo
        val previews = offChainInfoRepository.referendumPreviews(chain)
        val firstPost = previews[0]
        val details = offChainInfoRepository.referendumDetails(firstPost.referendumId, chain)
        print(details)
    }
}

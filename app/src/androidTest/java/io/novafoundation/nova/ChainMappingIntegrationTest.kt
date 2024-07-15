package io.novafoundation.nova

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import androidx.test.platform.app.InstrumentationRegistry
import dagger.Component
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.RemoteToDomainChainMapperFacade
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainExplorerToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainExternalApiToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainLocalToChain
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainNodeToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapExternalApisToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapNodeSelectionPreferencesToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteChainToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteExplorersToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteNodesToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote
import io.novafoundation.nova.test_shared.assertAllItemsEquals
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@Component(
    dependencies = [
        CommonApi::class,
        RuntimeApi::class
    ]
)
interface MappingTestAppComponent {

    fun inject(test: ChainMappingIntegrationTest)
}

@RunWith(AndroidJUnit4::class)
class ChainMappingIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val featureContainer = context as FeatureContainer

    @Inject
    lateinit var networkApiCreator: NetworkApiCreator

    @Inject
    lateinit var remoteToDomainChainMapperFacade: RemoteToDomainChainMapperFacade

    lateinit var chainFetcher: ChainFetcher

    private val gson = Gson()

    @Before
    fun prepare() {
        val component = DaggerMappingTestAppComponent.builder()
            .commonApi(featureContainer.commonApi())
            .runtimeApi(featureContainer.getFeature(RuntimeApi::class.java))
            .build()

        component.inject(this)

        chainFetcher = networkApiCreator.create(ChainFetcher::class.java)
    }

    @Test
    fun testChainMappingIsMatch() {
        runBlocking {
            val chainsRemote = chainFetcher.getChains()

            val remoteToDomain = chainsRemote.map { mapRemoteToDomain(it) }
            val remoteToLocalToDomain = chainsRemote.map { mapRemoteToLocalToDomain(it) }
            val domainToLocalToDomain = remoteToDomain.map { mapDomainToLocalToDomain(it) }

            assertAllItemsEquals(listOf(remoteToDomain, remoteToLocalToDomain, domainToLocalToDomain))
        }
    }

    private fun mapRemoteToLocalToDomain(chainRemote: ChainRemote): Chain {
        val chainLocal = mapRemoteChainToLocal(chainRemote, null, ChainLocal.Source.DEFAULT, gson)
        val assetsLocal = chainRemote.assets.map { mapRemoteAssetToLocal(chainRemote, it, gson, isEnabled = true) }
        val nodesLocal = mapRemoteNodesToLocal(chainRemote)
        val explorersLocal = mapRemoteExplorersToLocal(chainRemote)
        val externalApisLocal = mapExternalApisToLocal(chainRemote)

        return mapChainLocalToChain(
            chainLocal = chainLocal,
            nodesLocal = nodesLocal,
            nodeSelectionPreferences = NodeSelectionPreferencesLocal(chainLocal.id, autoBalanceEnabled = true, selectedNodeUrl = null),
            assetsLocal = assetsLocal,
            explorersLocal = explorersLocal,
            externalApisLocal = externalApisLocal,
            gson = gson
        )
    }

    private fun mapRemoteToDomain(chainRemote: ChainRemote): Chain {
        return remoteToDomainChainMapperFacade.mapRemoteChainToDomain(chainRemote, Chain.Source.DEFAULT)
    }

    private fun mapDomainToLocalToDomain(chain: Chain): Chain {
        val chainLocal = mapChainToLocal(chain, gson)
        val nodesLocal = chain.nodes.nodes.map { mapChainNodeToLocal(it) }
        val nodeSelectionPreferencesLocal = mapNodeSelectionPreferencesToLocal(chain)
        val assetsLocal = chain.assets.map { mapChainAssetToLocal(it, gson) }
        val explorersLocal = chain.explorers.map { mapChainExplorerToLocal(it) }
        val externalApisLocal = chain.externalApis.map { mapChainExternalApiToLocal(gson, chain.id, it) }

        return mapChainLocalToChain(
            chainLocal = chainLocal,
            nodesLocal = nodesLocal,
            nodeSelectionPreferences = nodeSelectionPreferencesLocal,
            assetsLocal = assetsLocal,
            explorersLocal = explorersLocal,
            externalApisLocal = externalApisLocal,
            gson = gson
        )
    }
}

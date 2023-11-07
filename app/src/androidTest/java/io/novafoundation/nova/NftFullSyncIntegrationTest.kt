package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_api.data.model.isFullySynced
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import org.junit.Test

class NftFullSyncIntegrationTest {

    private val nftApi = FeatureUtils.getFeature<NftFeatureApi>(
        ApplicationProvider.getApplicationContext<Context>(),
        NftFeatureApi::class.java
    )

    private val accountApi = FeatureUtils.getFeature<AccountFeatureApi>(
        ApplicationProvider.getApplicationContext<Context>(),
        AccountFeatureApi::class.java
    )

    private val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        RuntimeApi::class.java
    )

    private val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    @Test
    fun testFullSyncIntegration(): Unit = runBlocking {
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)

        val metaAccount = accountApi.accountUseCase().getSelectedMetaAccount()

        val nftRepository = nftApi.nftRepository

        nftRepository.initialNftSync(metaAccount, true)

        nftRepository.allNftWithMetadataFlow(metaAccount)
            .map { nfts -> nfts.filter { !it.isFullySynced } }
            .takeWhile { it.isNotEmpty() }
            .onEach { unsyncedNfts ->
                unsyncedNfts.forEach { nftRepository.fullNftSync(it) }
            }
            .onCompletion {
                print("Full sync done")
            }
            .launchIn(this)
    }
}

package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.feature_xcm_api.dryRun.model.OriginCaller
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import org.junit.Test

class DryRunIntegrationTest : BaseIntegrationTest() {

    private val xcmApi = FeatureUtils.getFeature<XcmFeatureApi>(
        ApplicationProvider.getApplicationContext<Context>(),
        XcmFeatureApi::class.java
    )

    private val dryRunApi = xcmApi.dryRunApi

    @Test
    fun testDryRunCall() = runTest {
        val polkadot = chainRegistry.polkadot()
        val polkadotRuntime = chainRegistry.getRuntime(polkadot.id)

        val result = dryRunApi.dryRunCall(
            originCaller = OriginCaller.System.Root,
            call = polkadotRuntime.composeCall(
                moduleName = Modules.SYSTEM,
                callName = "remark_with_event",
                args = emptyMap()
            ),
            chainId = polkadot.id
        )
        println(result)
    }
}

package io.novafoundation.nova.feature_governance_impl.presentation.common.share

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDeepLinkData
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

interface ShareReferendumMixin {
    val shareEvent: LiveData<Event<Uri>>

    fun shareReferendum(referendumId: BigInteger, chain: Chain, governanceType: Chain.Governance)
}

class RealShareReferendumMixin(
    private val referendumLinkConfigurator: ReferendumDetailsDeepLinkConfigurator
) : ShareReferendumMixin {

    private val _shareEvent = MutableLiveData<Event<Uri>>()
    override val shareEvent: LiveData<Event<Uri>> = _shareEvent

    override fun shareReferendum(referendumId: BigInteger, chain: Chain, governanceType: Chain.Governance) {
        val payload = ReferendumDeepLinkData(chain.id, referendumId, governanceType)

        val uri = referendumLinkConfigurator.configure(payload, type = DeepLinkConfigurator.Type.APP_LINK)
        _shareEvent.value = uri.event()
    }
}

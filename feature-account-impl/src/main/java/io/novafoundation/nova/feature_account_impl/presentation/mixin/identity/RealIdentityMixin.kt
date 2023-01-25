package io.novafoundation.nova.feature_account_impl.presentation.mixin.identity

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityModel
import kotlinx.coroutines.flow.MutableStateFlow

class RealIdentityMixinFactory(
    private val appLinksProvider: AppLinksProvider,
): IdentityMixin.Factory {

    override fun create(): IdentityMixin.Presentation {
        return RealIdentityMixin(appLinksProvider)
    }
}

private class RealIdentityMixin(
    private val appLinksProvider: AppLinksProvider,
): IdentityMixin.Presentation {

    override val openBrowserEvent = MutableLiveData<Event<String>>()
    override val openEmailEvent = MutableLiveData<Event<String>>()

    private val identityFlow = MutableStateFlow<IdentityModel?>(null)

    override fun setIdentity(identity: IdentityModel?) {
        identityFlow.value = identity
    }


    override fun emailClicked() = useIdentityField(IdentityModel::email) {
        openEmailEvent.value = it.event()
    }

    override fun twitterClicked() = useIdentityField(IdentityModel::twitter) {
        val link = appLinksProvider.getTwitterAccountUrl(it)

        openBrowserEvent.value = link.event()
    }

    override fun webClicked() = useIdentityField(IdentityModel::web) {
        openBrowserEvent.value = it.event()
    }

    private fun <R> useIdentityField(
        field: (IdentityModel) -> R?,
        action: (R) -> Unit
    ) {
        identityFlow.value?.let(field)?.let(action)
    }
}

package io.novafoundation.nova.feature_account_api.presenatation.actions

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.runtime.ext.accountUrlOf
import io.novafoundation.nova.runtime.ext.eventUrlOf
import io.novafoundation.nova.runtime.ext.extrinsicUrlOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ExternalActionsProvider(
    val clipboardManager: ClipboardManager,
    val resourceManager: ResourceManager,
) : ExternalActions.Presentation {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    override val showExternalActionsEvent = MutableLiveData<Event<ExternalActions.Payload>>()

    override fun viewExternalClicked(explorer: Chain.Explorer, type: ExternalActions.Type) {
        val url = when (type) {
            is ExternalActions.Type.Address -> explorer.accountUrlOf(type.address)
            is ExternalActions.Type.Event -> explorer.eventUrlOf(type.id)
            is ExternalActions.Type.Extrinsic -> explorer.extrinsicUrlOf(type.hash)
            is ExternalActions.Type.None -> null
        }

        url?.let { showBrowser(url) }
    }

    override fun showBrowser(url: String) {
        openBrowserEvent.value = Event(url)
    }

    override fun showExternalActions(type: ExternalActions.Type, chain: Chain) {
        val copyLabelRes = when (type) {
            is ExternalActions.Type.Address -> R.string.common_copy_address
            is ExternalActions.Type.Event -> R.string.common_copy_id
            is ExternalActions.Type.Extrinsic -> R.string.transaction_details_copy_hash
            is ExternalActions.Type.None -> null
        }

        val payload = ExternalActions.Payload(
            type = type,
            chain = chain,
            copyLabelRes = copyLabelRes
        )

        showExternalActionsEvent.value = Event(payload)
    }

    override fun copyAddress(address: String, messageShower: (message: String) -> Unit) {
        clipboardManager.addToClipboard(address)

        val message = resourceManager.getString(R.string.common_copied)

        messageShower.invoke(message)
    }
}

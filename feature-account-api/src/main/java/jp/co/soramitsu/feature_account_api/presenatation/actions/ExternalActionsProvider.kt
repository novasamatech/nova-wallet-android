package jp.co.soramitsu.feature_account_api.presenatation.actions

import androidx.lifecycle.MutableLiveData
import jp.co.soramitsu.common.R
import jp.co.soramitsu.common.resources.ClipboardManager
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.runtime.ext.accountUrlOf
import jp.co.soramitsu.runtime.ext.eventUrlOf
import jp.co.soramitsu.runtime.ext.extrinsicUrlOf
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

class ExternalActionsProvider(
    val clipboardManager: ClipboardManager,
    val resourceManager: ResourceManager
) : ExternalActions.Presentation {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    override val showExternalActionsEvent = MutableLiveData<Event<ExternalActions.Payload>>()

    override fun viewExternalClicked(explorer: Chain.Explorer, type: ExternalActions.Type) {
        val url = when (type) {
            is ExternalActions.Type.Address -> explorer.accountUrlOf(type.address)
            is ExternalActions.Type.Event -> explorer.eventUrlOf(type.id)
            is ExternalActions.Type.Extrinsic -> explorer.extrinsicUrlOf(type.hash)
        }

        showBrowser(url)
    }

    override fun showBrowser(url: String) {
        openBrowserEvent.value = Event(url)
    }

    override fun showExternalActions(type: ExternalActions.Type, chain: Chain) {
        val copyLabelRes = when (type) {
            is ExternalActions.Type.Address -> R.string.common_copy_address
            is ExternalActions.Type.Event -> R.string.common_copy_id
            is ExternalActions.Type.Extrinsic -> R.string.transaction_details_copy_hash
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

package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create

import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_impl.databinding.PageCreateWatchWalletBinding
import kotlinx.coroutines.flow.MutableStateFlow

class WatchOnlyModePageModel(
    val modeName: String,
    val config: Config,
) {
    data class Config(
        val isEditable: Boolean,
        val nameInput: MutableStateFlow<String>,
        val substrateAddressInput: AddressInputMixin,
        val evmAddressInput: AddressInputMixin
    )
}

class CreateWatchWalletPagerAdapter(
    private val pages: List<WatchOnlyModePageModel>,
    private val screenScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<WatchOnlyPageViewHolder>() {

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): WatchOnlyPageViewHolder {
        val page = pages[position]

        val binder = PageCreateWatchWalletBinding.inflate(parent.inflater(), parent, false)
        return WatchOnlyPageViewHolder(binder, page.config, screenScope)
    }

    override fun onBindViewHolder(holder: WatchOnlyPageViewHolder, position: Int) {
        // We setup views in onCreateViewHolder only once. So no need to bind anything
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun getPageTitle(position: Int): CharSequence {
        return pages[position].modeName
    }
}

class WatchOnlyPageViewHolder(
    binder: PageCreateWatchWalletBinding,
    config: WatchOnlyModePageModel.Config,
    screenScope: LifecycleCoroutineScope
) : ViewHolder(binder.root) {

    init {
        binder.createWatchWalletName.isEnabled = config.isEditable
        binder.createWatchWalletSubstrateAddress.isEnabled = config.isEditable
        binder.createWatchWalletEvmAddress.isEnabled = config.isEditable

        setupAddressInput(screenScope, config.substrateAddressInput, binder.createWatchWalletSubstrateAddress)
        setupAddressInput(screenScope, config.evmAddressInput, binder.createWatchWalletEvmAddress)

        binder.createWatchWalletName.bindTo(config.nameInput, screenScope)
    }
}

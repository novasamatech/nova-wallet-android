package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.view

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.BaseDynamicListBottomSheet
import io.novafoundation.nova.feature_wallet_connect_impl.R

class WCNetworksBottomSheet(
    context: Context,
    private val data: List<WCNetworkListModel>,
) : BaseDynamicListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.common_networks)

        recyclerView.setHasFixedSize(true)

        val adapter = WCNetworksAdapter()
        recyclerView.adapter = adapter
        adapter.submitList(data)
    }
}

package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items.NetworkListRvItem
import kotlinx.coroutines.flow.Flow

abstract class NetworkListViewModel : BaseViewModel() {


    abstract val networkList: Flow<List<NetworkListRvItem>>

}

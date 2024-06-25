package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.AddedNetworkListFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.defaultNetworks.ExistingNetworkListFragment

class NetworkManagementPagerAdapter(private val fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ExistingNetworkListFragment()
            1 -> AddedNetworkListFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> fragment.getString(R.string.network_management_default_page_title)
            1 -> fragment.getString(R.string.network_management_added_page_title)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}

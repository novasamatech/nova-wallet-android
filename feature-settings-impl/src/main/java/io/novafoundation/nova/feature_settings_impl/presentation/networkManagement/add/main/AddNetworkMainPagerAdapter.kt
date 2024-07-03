package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.AddNetworkFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.AddNetworkPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.AddedNetworkListFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.defaultNetworks.ExistingNetworkListFragment

class AddNetworkMainPagerAdapter(private val fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AddNetworkFragment().addPayloadMode(AddNetworkPayload.Mode.Substrate)
            1 -> AddNetworkFragment().addPayloadMode(AddNetworkPayload.Mode.EVM)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> fragment.getString(R.string.common_substrate)
            1 -> fragment.getString(R.string.common_evm)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    private fun AddNetworkFragment.addPayloadMode(mode: AddNetworkPayload.Mode): AddNetworkFragment {
        this.arguments = AddNetworkFragment.getBundle(AddNetworkPayload(mode, prefilledData = null))

        return this
    }
}

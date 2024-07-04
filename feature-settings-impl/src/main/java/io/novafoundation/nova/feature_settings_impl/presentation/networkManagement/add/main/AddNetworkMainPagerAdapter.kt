package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.AddNetworkFragment

class AddNetworkMainPagerAdapter(
    private val fragment: Fragment,
    private val payloadForSinglePage: AddNetworkPayload?
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return if (payloadForSinglePage == null) {
            2
        } else {
            1
        }
    }

    override fun createFragment(position: Int): Fragment {
        return if (payloadForSinglePage == null) {
            when (position) {
                0 -> AddNetworkFragment().addPayloadMode(AddNetworkPayload.Mode.SUBSTRATE)
                1 -> AddNetworkFragment().addPayloadMode(AddNetworkPayload.Mode.EVM)
                else -> throw IllegalArgumentException("Invalid position")
            }
        } else {
            AddNetworkFragment().addPayload(payloadForSinglePage)
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return if (payloadForSinglePage == null) {
            when (position) {
                0 -> fragment.getString(R.string.common_substrate)
                1 -> fragment.getString(R.string.common_evm)
                else -> throw IllegalArgumentException("Invalid position")
            }
        } else {
            "" // For single page we hide TabLayout
        }
    }

    private fun AddNetworkFragment.addPayloadMode(mode: AddNetworkPayload.Mode): AddNetworkFragment {
        return addPayload(AddNetworkPayload(mode, prefilledData = null))
    }

    private fun AddNetworkFragment.addPayload(mode: AddNetworkPayload): AddNetworkFragment {
        this.arguments = AddNetworkFragment.getBundle(mode)

        return this
    }
}

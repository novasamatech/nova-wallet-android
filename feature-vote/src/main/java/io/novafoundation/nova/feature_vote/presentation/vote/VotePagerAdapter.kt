package io.novafoundation.nova.feature_vote.presentation.vote

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.novafoundation.nova.feature_vote.R
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

class VotePagerAdapter(private val fragment: Fragment, private val router: VoteRouter) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> router.getDemocracyFragment()
            1 -> router.getCrowdloansFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> fragment.getString(R.string.common_governance)
            1 -> fragment.getString(R.string.crowdloan_crowdloan)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}

package io.novafoundation.nova.feature_vote.presentation.vote

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

class VotePagerAdapter(fragment: Fragment, private val router: VoteRouter) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 1
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> router.getDemocracyFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}

package io.novafoundation.nova.feature_pay_impl.presentation.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter

class PayPagerAdapter(private val fragment: Fragment, private val router: PayRouter) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        // TODO
        return Fragment()
    }

    fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> fragment.getString(R.string.pay_tab_spend)
            1 -> fragment.getString(R.string.pay_tab_shop)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}

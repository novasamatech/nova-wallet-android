package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_network_info.view.stakingNetworkCollapsibleView
import kotlinx.android.synthetic.main.view_network_info.view.stakingNetworkInfoTitle

private const val ANIMATION_DURATION = 220L

class NetworkInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    enum class State {
        EXPANDED,
        COLLAPSED
    }

    private var currentState = State.COLLAPSED

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        NetworkInfoAdapter()
    }

    init {
        View.inflate(context, R.layout.view_network_info, this)

        with(context) {
            background = getBlockDrawable()
        }

        orientation = VERTICAL

        stakingNetworkCollapsibleView.adapter = adapter
        stakingNetworkCollapsibleView.itemAnimator = null
    }

    fun setState(state: NetworkInfoState?) {
        if (state == null) {
            makeGone()
            return
        }

        makeVisible()

        setExpanded(state.expanded)

        adapter.submitList(state.actions)
    }

    fun onExpandClicked(listener: OnClickListener) {
        stakingNetworkInfoTitle.setOnClickListener(listener)
    }

    private fun setExpanded(expanded: Boolean) {
        if (expanded) {
            expand()
        } else {
            collapse()
        }
    }

    private fun collapse() {
        if (currentState == State.COLLAPSED) return

        stakingNetworkInfoTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_down, 0)
        currentState = State.COLLAPSED
        stakingNetworkCollapsibleView.animate()
            .setDuration(ANIMATION_DURATION)
            .alpha(0f)
            .withEndAction { stakingNetworkCollapsibleView.makeGone() }
    }

    private fun expand() {
        if (currentState == State.EXPANDED) return

        stakingNetworkInfoTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_up, 0)
        stakingNetworkCollapsibleView.makeVisible()
        currentState = State.EXPANDED
        stakingNetworkCollapsibleView.animate()
            .setDuration(ANIMATION_DURATION)
            .alpha(1f)
    }
}

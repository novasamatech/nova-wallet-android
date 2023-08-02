package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_your_pool.view.yourPoolContentGroup
import kotlinx.android.synthetic.main.view_your_pool.view.yourPoolIcon
import kotlinx.android.synthetic.main.view_your_pool.view.yourPoolLoadingGroup
import kotlinx.android.synthetic.main.view_your_pool.view.yourPoolName
import kotlinx.android.synthetic.main.view_your_pool.view.yourPoolTitle

class YourPoolView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val imageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        View.inflate(context, R.layout.view_your_pool, this)

        with(context) {
            background = getBlockDrawable()
        }
    }

    fun setOnPoolClicked(listener: (View) -> Unit) {
        setOnClickListener(listener)
    }

    fun showYourPoolState(yourPoolState: LoadingState<YourPoolComponentState>) {
        when(yourPoolState) {
            is LoadingState.Loaded -> showLoaded(yourPoolState.data)
            is LoadingState.Loading -> showLoading()
        }
    }

    private fun showLoaded(yourPoolState: YourPoolComponentState) {
        yourPoolContentGroup.makeVisible()
        yourPoolLoadingGroup.makeGone()

        yourPoolIcon.setIcon(yourPoolState.poolIcon, imageLoader)
        yourPoolName.text = yourPoolState.poolAccount.nameOrAddress
        yourPoolTitle.text = yourPoolState.title
    }

    private fun showLoading() {
        yourPoolContentGroup.makeGone()
        yourPoolLoadingGroup.makeVisible()
    }
}

package io.novafoundation.nova.common.view

import io.novafoundation.nova.common.utils.setImageResourceOrHide
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableWithRipple
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewWalletAppBarBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.firstViewInstanceInHierarchy
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.onCurrentPageViewChange
import io.novafoundation.nova.common.utils.updatePadding

class WalletAppBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions {

    override val providedContext: Context = context

    private val binder = ViewWalletAppBarBinding.inflate(inflater(), this)

    init {
        binder.walletAppBarSelectedWallet.background = context.getRoundedCornerDrawableWithRipple(null, cornerSizeInDp = 100)
    }

    fun setWalletConnectActive(hasConnections: Boolean) {
        if (hasConnections) {
            binder.walletAppBarWalletConnect.setImageResource(R.drawable.ic_wallet_connect_scan_indicator)
        } else {
            binder.walletAppBarWalletConnect.setImageResource(R.drawable.ic_wallet_connect_scan)
        }
    }

    fun setSelectedWallet(@DrawableRes walletTypeIcon: Int?, walletName: String) {
        binder.walletAppBarSelectedWalletTypeIcon.setImageResourceOrHide(walletTypeIcon)
        binder.walletAppBarSelectedWalletName.text = walletName

        if (walletTypeIcon == null) {
            binder.walletAppBarSelectedWallet.updatePadding(start = 12.dp)
        } else {
            binder.walletAppBarSelectedWallet.updatePadding(start = 8.dp)
        }
    }

    fun onWalletConnectClick(listener: OnClickListener) {
        binder.walletAppBarWalletConnect.setOnClickListener(listener)
    }

    fun onSettingsClick(listener: OnClickListener) {
        binder.walletAppBarSettings.setOnClickListener(listener)
    }

    fun onWalletClick(listener: OnClickListener) {
        binder.walletAppBarSelectedWallet.setOnClickListener(listener)
    }
}

fun View.bindWithViewPager2(
    viewPager2: ViewPager2,
    onScrollDrawableRes: Int,
    offscreenPageLimit: Int = 2
) {
    val drawable = ResourcesCompat.getDrawable(resources, onScrollDrawableRes, context.theme) ?: return

    viewPager2.offscreenPageLimit = offscreenPageLimit

    var oldRecyclerView: RecyclerView? = null

    viewPager2.onCurrentPageViewChange {
        val pageRecyclerView = it.firstViewInstanceInHierarchy<RecyclerView>()
        oldRecyclerView?.unbindListener()
        if (pageRecyclerView != null) {
            this.bindWithRecyclerView(pageRecyclerView, drawable)
        } else {
            this.unbindBackground(drawable)
        }

        oldRecyclerView = pageRecyclerView
    }
}

fun View.bindWithRecyclerView(recyclerView: RecyclerView, onScrollDrawableRes: Int) {
    val drawable = ResourcesCompat.getDrawable(resources, onScrollDrawableRes, context.theme) ?: return
    bindWithRecyclerView(recyclerView, drawable)
}

fun View.bindWithRecyclerView(recyclerView: RecyclerView, drawable: Drawable) {
    drawable.alpha = 0

    var oldCanScrollVertically = recyclerView.canScrollVertically(-1)
    animateBackground(drawable, oldCanScrollVertically)

    recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
        val newCanScrollVertically = recyclerView.canScrollVertically(-1)

        if (oldCanScrollVertically != newCanScrollVertically) {
            oldCanScrollVertically = newCanScrollVertically
            animateBackground(drawable, newCanScrollVertically)
        }
    }
}

private fun RecyclerView.unbindListener() {
    setOnScrollChangeListener(null)
}

private fun View.unbindBackground(drawable: Drawable) {
    runBackgroundAnimation(0, drawable)
}

private fun View.animateBackground(drawable: Drawable, showBackground: Boolean) {
    if (showBackground) {
        runBackgroundAnimation(255, drawable)
    } else {
        runBackgroundAnimation(0, drawable)
    }
}

fun View.runBackgroundAnimation(toAlpha: Int, backgroundDrawable: Drawable) {
    background = backgroundDrawable
    val currentAlpha = backgroundDrawable.alpha
    animate()
        .setUpdateListener {
            background.alpha = (currentAlpha + (toAlpha - currentAlpha) * it.animatedFraction).toInt()
        }
        .setDuration(200)
        .start()
}

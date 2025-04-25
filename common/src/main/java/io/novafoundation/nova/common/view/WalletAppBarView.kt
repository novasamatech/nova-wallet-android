package io.novafoundation.nova.common.view

import io.novafoundation.nova.common.utils.setImageResourceOrHide
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableWithRipple
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewWalletAppBarBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
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

fun WalletAppBarView.bindWithRecyclerView(recyclerView: RecyclerView, onScrollDrawableRes: Int) {
    val drawable = ResourcesCompat.getDrawable(resources, onScrollDrawableRes, context.theme) ?: return
    drawable.alpha = 0

    var oldCanScrollVertically = recyclerView.canScrollVertically(-1)
    recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
        val newCanScrollVertically = recyclerView.canScrollVertically(-1)

        if (oldCanScrollVertically != newCanScrollVertically) {
            oldCanScrollVertically = newCanScrollVertically

            if (newCanScrollVertically) {
                runBackgroundAnimation(255, drawable)
            } else {
                runBackgroundAnimation(0, drawable)
            }
        }
    }
}

private fun WalletAppBarView.runBackgroundAnimation(toAlpha: Int, backgroundDrawable: Drawable) {
    background = backgroundDrawable
    val currentAlpha = backgroundDrawable.alpha
    animate()
        .setUpdateListener {
            background.alpha = (currentAlpha + (toAlpha - currentAlpha) * it.animatedFraction).toInt()
        }
        .setDuration(200)
        .start()
}

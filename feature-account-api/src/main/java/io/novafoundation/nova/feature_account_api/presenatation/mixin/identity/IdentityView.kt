package io.novafoundation.nova.feature_account_api.presenatation.mixin.identity

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.view.TableView
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.R
import kotlinx.android.synthetic.main.view_identity.view.viewIdentityElementName
import kotlinx.android.synthetic.main.view_identity.view.viewIdentityEmail
import kotlinx.android.synthetic.main.view_identity.view.viewIdentityLegalName
import kotlinx.android.synthetic.main.view_identity.view.viewIdentityTwitter
import kotlinx.android.synthetic.main.view_identity.view.viewIdentityWeb

class IdentityView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TableView(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_identity, this)

        setTitle(context.getString(R.string.identity_title))
    }

    fun setModel(identityModel: IdentityModel) = with(identityModel) {
        viewIdentityLegalName.showValueOrHide(legal)
        viewIdentityEmail.showValueOrHide(email)
        viewIdentityTwitter.showValueOrHide(twitter)
        viewIdentityElementName.showValueOrHide(matrix)
        viewIdentityWeb.showValueOrHide(web)
    }

    fun onEmailClicked(onClick: () -> Unit) {
        viewIdentityEmail.setOnClickListener(onClick)
    }

    fun onWebClicked(onClick: () -> Unit) {
        viewIdentityWeb.setOnClickListener(onClick)
    }

    fun onTwitterClicked(onClick: () -> Unit) {
        viewIdentityTwitter.setOnClickListener(onClick)
    }

    private fun View.setOnClickListener(onClick: () -> Unit) {
        setOnClickListener { onClick() }
    }
}

fun IdentityView.setModelOrHide(identityModel: IdentityModel?) = letOrHide(identityModel, ::setModel)

package io.novafoundation.nova.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.app.di.app.AppComponent
import io.novafoundation.nova.app.di.deps.FeatureHolderManager
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.feature_wallet_connect_impl.BuildConfig
import javax.inject.Inject


private const val WC_REDIRECT_URL = "novawallet://request"

open class App : Application(), FeatureContainer {

    @Inject
    lateinit var featureHolderManager: FeatureHolderManager

    private lateinit var appComponent: AppComponent

    private val languagesHolder: LanguagesHolder = LanguagesHolder()

    override fun attachBaseContext(base: Context) {
        val contextManager = ContextManager.getInstanceOrInit(base, languagesHolder)
        super.attachBaseContext(contextManager.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val contextManager = ContextManager.getInstanceOrInit(this, languagesHolder)
        contextManager.setLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        val contextManger = ContextManager.getInstanceOrInit(this, languagesHolder)

        appComponent = io.novafoundation.nova.app.di.app.DaggerAppComponent
            .builder()
            .application(this)
            .contextManager(contextManger)
            .build()

        appComponent.inject(this)

        initializeWaleltConnect()
    }

    override fun <T> getFeature(key: Class<*>): T {
        return featureHolderManager.getFeature<T>(key)!!
    }

    override fun releaseFeature(key: Class<*>) {
        featureHolderManager.releaseFeature(key)
    }

    override fun commonApi(): CommonApi {
        return appComponent
    }

    private fun initializeWaleltConnect() {
        val projectId = BuildConfig.WALLET_CONNECT_PROJECT_ID
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=$projectId"
        val connectionType = ConnectionType.MANUAL
        val appMetaData = Core.Model.AppMetaData(
            name = "Nova Wallet",
            description = "Next-gen wallet for Polkadot and Kusama ecosystem",
            url = "https://novawallet.io/",
            icons = listOf("https://raw.githubusercontent.com/novasamatech/branding/master/logos/Nova_Wallet_Star_Color.png"),
            redirect = WC_REDIRECT_URL
        )

        CoreClient.initialize(relayServerUrl = serverUrl, connectionType = connectionType, application = this, metaData = appMetaData) { error ->
            // TODO maybe re-initialize client
        }

        val initParams = Wallet.Params.Init(core = CoreClient)

        Web3Wallet.initialize(initParams) { error ->
            // TODO maybe re-initialize client
        }
    }
}

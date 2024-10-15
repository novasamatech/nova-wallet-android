package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig

class NovaCardWebPageProvider(
    private val widgetId: String,
    private val setupConfig: CardSetupConfig
) {

    private val containerId = "widget-container"
    private val scriptUrl = getScriptUrl()
    private val callbackName: String = "NovaCallback"

    fun getCallbackName() = callbackName

    fun getPage(): String {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            
            <style>
                body {
                    margin: 0;
                    padding: 0;
                }
            </style>
        </head>
        <body>
            <div id="$containerId"></div>
        
            <script src="$scriptUrl"></script>
        </body>
        </html>
        """.trimIndent()
    }

    fun getJsScript(): String {
        return """
            mercuryoWidget.run({ 
                widgetId: '$widgetId',
                host: document.getElementById('$containerId'),
                type: 'sell',
                currency: '${setupConfig.spendToken.symbol.value}',
                fiatCurrency: 'EUR',
                paymentMethod: 'fiat_card_open',
                theme: 'nova',
                width: '100%',
                height: window.innerHeight,
                hideRefundAddress: true,
                refundAddress: '${setupConfig.refundAddress}',
                fixPaymentMethod: true,
                showSpendCardDetails: true,
                onStatusChange: data => {
                    $callbackName.onStatusChange(JSON.stringify(data));
                },
                onSellTransferEnabled: data => {
                    $callbackName.onSellTransferEnabled(JSON.stringify(data));
                }
            });
        """.trimIndent()
    }

    private fun getScriptUrl(): String {
        if (BuildConfig.DEBUG) {
            return "https://sandbox-exchange.mrcr.io/embed.2.0.js"
        } else {
            return "https://widget.mercuryo.io/embed.2.0.js"
        }
    }
}

package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig

class NovaCardWebPageProvider(
    private val widgetId: String,
    private val setupConfig: CardSetupConfig
) {

    private val containerId = "widget-container"
    private val callbackName: String = "NovaCallback"

    fun getCallbackName() = callbackName

    fun getPage(): String {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body>
            <div id="$containerId"></div>
        
            <script src="https://widget.mercuryo.io/embed.2.0.js"></script>
        </body>
        </html>
        """.trimIndent()
    }

    fun getJsScript(): String {
        return """
            mercuryoWidget.run({ 
                widgetId: '$widgetId',
                host: document.getElementById('widget-container'),
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
}

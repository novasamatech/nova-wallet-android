package io.novafoundation.nova.common.otherModules

import android.net.Uri
import io.novafoundation.nova.common.utils.bus.BaseRequestBus
import io.novafoundation.nova.common.utils.bus.RequestBus
import io.novafoundation.nova.common.otherModules.HandleDeeplinkEventBus.Response
import io.novafoundation.nova.common.otherModules.HandleDeeplinkEventBus.Request

class HandleDeeplinkEventBus : BaseRequestBus<Request, Response>() {

    class Request(val uri: Uri) : RequestBus.Request

    class Response(val uriHandled: Boolean) : RequestBus.Response
}

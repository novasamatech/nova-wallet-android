window.addEventListener("message", ({ data, source }) => {
      // only allow messages from our window, by the loader
      if (source !== window) {
        return;
      }

      let dataJson = JSON.stringify(data)
      console.log(`Got message: ${dataJson}`)

      if (data.origin === "dapp-request") {
      // should be in tact with PolkadotJsExtension.kt
        Nova_PolkadotJs.onNewMessage(dataJson)
      }
    });
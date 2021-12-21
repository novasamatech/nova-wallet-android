window.addEventListener("message", ({ data, source }) => {
      console.log(`Got extensionRequest: {data}`)

      // only allow messages from our window, by the loader
      if (source !== window) {
        return;
      }

      if (data.origin === "dapp-request") {
      // should be in tact with PolkadotJsExtension.kt
        Nova.onNewMessage(JSON.stringify(data))
      }
    });
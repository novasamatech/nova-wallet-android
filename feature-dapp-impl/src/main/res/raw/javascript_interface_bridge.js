window.addEventListener("message", ({ data, source }) => {
      // only allow messages from our window, by the loader
      if (source !== window) {
        return;
      }

      if (data.origin === "dapp-request") {
      // should be in tact with PolkadotJsExtension.kt
        Nova.onNewMessage(data)
      }
    });
window.addEventListener("message", ({ data, source }) => {
      // only allow messages from our window, by the loader
      if (source !== window) {
        return;
      }

      if (data.origin === "dapp-request") {
        Android.onNewMessage(data)
      }
    });
Android TV HomeKit
===

HomeKit service for Android TVs. Currently only works with Philips Android TVs and requires a hardcoded
username/password. Uses some bits I reverse engineered from the XTv service (the API on ports 1625-1627).

### TODO

- [ ] Setup interface
    - I have no idea how to do this.
    - [ ] Walk through creating a pairing
    - [ ] Show a QR code and random PIN (generated every time the app is opened) to pair with a HomeKit client
    - [ ] Button to reset pairings
- [ ] Disable unauthenticated requests for the release build
- [ ] Read the username and password from a file created by the setup interface

### Requirements

To build:

- Java
- Android SDK (API 25)

To run:

- Android 8 Oreo
- A compatible TV:
    - Philips TV with xtv API 6.0 or later (see [usage](#usage))

This requires Android 8.0 (API 26) but targets Android 7.1 (API 25) because Android 8 add restrictions on apps running
in the background. Android 8 is only required for
[Async Http Client](https://github.com/AsyncHttpClient/async-http-client) - for Android 7 support this could probably
be replaced with another library.
[Other TVs can be supported by returning a different implementation in HomeKitService](app/src/main/java/uk/org/fancy/AndroidTvHomeKit/HomeKitService.java#L39).

Usage
---

At the moment there's no UI and TV-specific API credentials are hardcoded in the app.

1. Create a pairing for the TV. [You can use Pylips for this.](https://github.com/eslavnov/pylips)
2. Add the username and password to
    [app/src/main/java/uk/org/fancy/AndroidTvHomeKit/Philips/Television.java](app/src/main/java/uk/org/fancy/AndroidTvHomeKit/Philips/Television.java#L32-33).
    ```java
    private final String username = "...";
    private final String password = "...";
    ```
3. Disable unauthenticated requests. (Optional but recommended if you don't need to access the HAP server directly.)

    In
    [app/src/main/java/uk/org/fancy/AndroidTvHomeKit/HomeKitService.java](app/src/main/java/uk/org/fancy/AndroidTvHomeKit/HomeKitService.java#L56)
    replace
    ```java
    // Allow unauthenticated requests
    accessoryServer.getRoot().getRegistry().setAllowUnauthenticatedRequests(true);
    ```
    with
    ```java
    // Allow unauthenticated requests
    accessoryServer.getRoot().getRegistry().setAllowUnauthenticatedRequests(false);
    ```
4. Build the app.
    ```
    ./gradlew buildDebug packageDebug
    ```
5. Install the app.
    ```
    # -r updates the app if you already have it installed
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```
6. Start the HomeKit service.
    ```
    # This usually only has to be done after installing for the first time
    # It will start when Android starts and when installing and the service is running
    adb shell am start-service uk.org.fancy.AndroidTvHomeKit/.HomeKitService
    ```

#### Why do I need to create a pairing?

The permissions the service needs are only available to system apps and apps signed with Philips' key, so the TV's
public REST API is used instead.

#### Why does this use vulnerabilities in Philips' API to generate a nonce for digest authentication?

1. The API uses HTTPS so assuming each TV generates it's own certificate and clients watch the certificate to make sure
    it doesn't change the digest authentication isn't necessary.
2. Restlet's digest authentication doesn't seem very stable and crashes a lot in a way that the service is still able
    to run so it has to be restarted manually.

### Custom characteristics

UUID                                 | Name                     | -
-------------------------------------|--------------------------|----
CA7897C2-4B46-448D-AF68-29C686B730F4 | TV Input Framework ID    | String, read only
CA7897C3-4B46-448D-AF68-29C686B730F4 | Application ID           | String, read only
CA7897C4-4B46-448D-AF68-29C686B730F4 | Ambilight Colour Sync    | Enum, 0-19, read/write/event
CA7897C5-4B46-448D-AF68-29C686B730F4 | Colour Variation         | Integer, 0-255, read/write/event
CA7897C6-4B46-448D-AF68-29C686B730F4 | Colour Variation Speed   | Integer, 0-255, read/write/event

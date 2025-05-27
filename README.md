# AwayText 📱

AwayText is a simple and customizable Android app that automatically replies to incoming messages when you're busy, driving, or just need some time away from your phone.

## ✨ Features

- 📨 Auto-reply to incoming SMS with a custom message
- 🕒 Enable or disable AwayText with one tap
- 📱 Lightweight and battery-efficient

## 💼 TODO
- 📋 Custom status messages for different scenarios (e.g. "Driving", "In a meeting", "Sleeping")

## 📦 Installation

You can download the latest APK from the [Releases](https://github.com/jay-bman725/away-text/releases) page.

**Note:** You may need to enable "Install from Unknown Sources" in your device settings to install the APK manually.

## 🛠️ Development

### Requirements

- Android Studio
- Java or Kotlin knowledge
- Android SDK version 21+

### Build Instructions

```bash
git clone https://github.com/jay-bman725/away-text.git
cd away-text
# Open in Android Studio and build the project
````

### Signing the APK

If you're building for release, you'll need to configure your keystore and secrets:

1. Add your keystore file and base64 encode it if using GitHub Actions.
2. Set the following secrets in your repository:

   * `KEYSTORE_BASE64`
   * `KEYSTORE_PASSWORD`
   * `KEY_ALIAS`
   * `KEY_PASSWORD`

## 🚀 GitHub Actions

This project uses GitHub Actions to automatically build and release a new APK when a new version is pushed.

* Version is read from the `version` file in the root of the repo.
* A release is created and tagged with the version number.
* Source code and APK are attached to the release.

## 📄 License

This project is licensed under the MIT License. See [`LICENSE`](LICENSE) for more details.

## 🙌 Contributions

Pull requests are welcome! If you have suggestions or feature ideas, feel free to [open an issue](https://github.com/jay-bman725/away-text/issues).

---

Jay Berryman
[https://www.jaysapps.com](https://www.jaysapps.com)

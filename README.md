# AwayText 📱

AwayText is a simple and customizable Android app that automatically replies to incoming messages when you're busy, driving, or just need some time away from your phone.

## ✨ Features

- 📨 Auto-reply to incoming SMS and RCS messages with a custom message
- 🚀 Intelligent message type detection (uses RCS when available, falls back to SMS)
- 🕒 Enable or disable AwayText with one tap
- 📱 Lightweight and battery-efficient
- 🔔 Do Not Disturb integration for automatic activation
- ⏱️ 5-minute cooldown between replies to the same contact

## 💼 TODO
- 📋 Custom status messages for different scenarios (e.g. "Driving", "In a meeting", "Sleeping")

## 📦 Installation

You can download the latest APK from the [Releases](https://github.com/jay-bman725/away-text/releases) page.

**Note:** You may need to enable "Install from Unknown Sources" in your device settings to install the APK manually.

## 📱 Feature Details

### Do Not Disturb (DND) Integration

#### Overview
AwayText automatically integrates with your phone's Do Not Disturb settings to provide intelligent auto-reply functionality. When Do Not Disturb is enabled, AwayText can automatically start monitoring and responding to messages.

#### Features

##### Auto-Start with DND (Optional)
- **What it does**: Automatically starts AwayText when Do Not Disturb mode is enabled
- **How to enable**: In the main screen, toggle "Auto-start with Do Not Disturb" to ON
- **Benefit**: Ensures you never miss responding to important messages during quiet hours, meetings, or focus time

##### Auto-Stop when DND Ends (Optional)
- **What it does**: Automatically stops AwayText when Do Not Disturb mode is disabled
- **How to enable**: In the main screen, toggle "Auto-stop when DND ends" to ON
- **Benefit**: Returns to normal messaging behavior when you're available again

#### How It Works

##### Permission Requirements
1. **Notification Policy Access**: Required to detect DND state changes
   - Granted during the onboarding process
   - Can be managed in Settings > Do Not Disturb Access

##### Technical Implementation
1. **DND Monitoring**: Uses `NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED` broadcast
2. **State Detection**: Monitors `currentInterruptionFilter` to detect DND activation
3. **Service Integration**: Automatically starts/stops `SmsMonitoringService`
4. **Persistent Settings**: User preferences are stored and respected across app restarts

##### DND States Detected
- **INTERRUPTION_FILTER_ALL**: Normal mode (DND off)
- **INTERRUPTION_FILTER_PRIORITY**: Priority only mode (DND on)
- **INTERRUPTION_FILTER_ALARMS**: Alarms only mode (DND on)
- **INTERRUPTION_FILTER_NONE**: Total silence mode (DND on)

#### User Experience

##### Setup Process
1. During onboarding, grant "Do Not Disturb Access" permission
2. DND auto-start is enabled by default when permission is granted
3. Users can toggle auto-start/auto-stop in the main screen

##### Visual Feedback
- DND settings card shows current configuration
- Real-time DND status indicator when active
- Toast notifications when auto-start/stop occurs
- Clear toggles for both auto-start and auto-stop features

##### Smart Behavior
- Only auto-starts if AwayText is not already running
- Respects user's manual start/stop preferences
- Works across app restarts and phone reboots
- Minimal battery impact through efficient broadcast monitoring

#### Configuration Options

Users can customize the DND integration behavior:

1. **Auto-start with DND**: Enable/disable automatic startup
2. **Auto-stop when DND ends**: Enable/disable automatic shutdown
3. **Manual override**: Users can still manually start/stop regardless of DND state

#### Privacy and Security
- Only monitors DND state changes, not DND settings or policies
- No access to notification content or other DND configurations
- Uses minimal system resources with efficient broadcast receivers
- All preferences stored locally on device

### Message Cooldown System

#### How It Works

##### 5-Minute Reply Cooldown
When AwayText is active, it automatically replies to incoming messages. To prevent sending multiple replies to the same person in quick succession, a **5-minute cooldown timer** is enforced for each contact.

- If someone sends you multiple messages within 5 minutes, they will receive only **one** auto-reply
- After 5 minutes have passed, the next message they send will trigger a new auto-reply
- This prevents message spam and creates a more natural conversation experience

##### Example Scenarios

**Scenario 1: Multiple messages in quick succession**  
1. John sends you a message at 3:00 PM
2. AwayText sends an auto-reply immediately
3. John sends another message at 3:02 PM
4. AwayText recognizes this is within the 5-minute window and does not respond
5. John sends a third message at 3:04 PM
6. Still within the cooldown window, no additional auto-reply is sent

**Scenario 2: Messages spaced over time**  
1. Sarah sends you a message at 2:00 PM
2. AwayText sends an auto-reply immediately
3. Sarah sends another message at 2:10 PM (after the 5-minute window)
4. AwayText sends a new auto-reply since the cooldown period has elapsed

##### Benefits of the Cooldown System

- **Prevents Annoying Spam:** No one wants to receive the same automated message multiple times in a row
- **Respects Your Contacts:** Makes the auto-reply experience less robotic and more respectful
- **Reduces Message Clutter:** Keeps your conversation history cleaner with fewer repeated messages
- **Saves Battery & Data:** Minimizes unnecessary message sending

##### Technical Implementation

The cooldown system uses a timestamp-based approach:
- Each sender's phone number is tracked with the most recent auto-reply timestamp
- Phone numbers are normalized (all formats of the same number are treated identically)
- Time comparisons ensure the 5-minute rule is strictly enforced

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
```

### Technical Implementation Details

#### Message Cooldown Implementation

##### Problem Fixed
The app was sending duplicate replies to the same person when they sent a message. This happened because there was no mechanism to track when a reply was last sent to a specific sender.

##### Solution
We've implemented a 5-minute cooldown period for auto-responses:

1. Added timestamp tracking in `AppPreferences.kt`:
   - `recordLastReplyTimestamp(sender)`: Stores the time when a reply was sent
   - `canSendReplyToSender(sender)`: Checks if 5 minutes have passed since last reply

2. Modified `MessageMonitoringService.kt` to:
   - Check the cooldown period before sending any auto-reply
   - Only send one auto-reply per sender within a 5-minute window
   - Log both sent and skipped auto-replies for debugging

3. Phone number normalization:
   - Added a helper function to normalize phone numbers (removes non-digits)
   - Ensures different formats of the same number are treated as identical

##### How It Works
When a message is received:
1. The app checks if it's previously replied to the sender within 5 minutes
2. If not, it sends an auto-reply and records the timestamp
3. If yes, it skips sending another auto-reply

This prevents message spam and ensures a more natural behavior for the auto-reply feature.

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

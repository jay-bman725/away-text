# Do Not Disturb (DND) Integration

## Overview
AwayText now automatically integrates with your phone's Do Not Disturb settings to provide intelligent auto-reply functionality. When Do Not Disturb is enabled, AwayText can automatically start monitoring and responding to messages.

## Features

### Auto-Start with DND
- **What it does**: Automatically starts AwayText when Do Not Disturb mode is enabled
- **How to enable**: In the main screen, toggle "Auto-start with Do Not Disturb" to ON
- **Benefit**: Ensures you never miss responding to important messages during quiet hours, meetings, or focus time

### Auto-Stop when DND Ends (Optional)
- **What it does**: Automatically stops AwayText when Do Not Disturb mode is disabled
- **How to enable**: In the main screen, toggle "Auto-stop when DND ends" to ON
- **Benefit**: Returns to normal messaging behavior when you're available again

## How It Works

### Permission Requirements
1. **Notification Policy Access**: Required to detect DND state changes
   - Granted during the onboarding process
   - Can be managed in Settings > Do Not Disturb Access

### Technical Implementation
1. **DND Monitoring**: Uses `NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED` broadcast
2. **State Detection**: Monitors `currentInterruptionFilter` to detect DND activation
3. **Service Integration**: Automatically starts/stops `SmsMonitoringService`
4. **Persistent Settings**: User preferences are stored and respected across app restarts

### DND States Detected
- **INTERRUPTION_FILTER_ALL**: Normal mode (DND off)
- **INTERRUPTION_FILTER_PRIORITY**: Priority only mode (DND on)
- **INTERRUPTION_FILTER_ALARMS**: Alarms only mode (DND on)
- **INTERRUPTION_FILTER_NONE**: Total silence mode (DND on)

## User Experience

### Setup Process
1. During onboarding, grant "Do Not Disturb Access" permission
2. DND auto-start is enabled by default when permission is granted
3. Users can toggle auto-start/auto-stop in the main screen

### Visual Feedback
- DND settings card shows current configuration
- Real-time DND status indicator when active
- Toast notifications when auto-start/stop occurs
- Clear toggles for both auto-start and auto-stop features

### Smart Behavior
- Only auto-starts if AwayText is not already running
- Respects user's manual start/stop preferences
- Works across app restarts and phone reboots
- Minimal battery impact through efficient broadcast monitoring

## Configuration Options

Users can customize the DND integration behavior:

1. **Auto-start with DND**: Enable/disable automatic startup
2. **Auto-stop when DND ends**: Enable/disable automatic shutdown
3. **Manual override**: Users can still manually start/stop regardless of DND state

## Privacy and Security
- Only monitors DND state changes, not DND settings or policies
- No access to notification content or other DND configurations
- Uses minimal system resources with efficient broadcast receivers
- All preferences stored locally on device

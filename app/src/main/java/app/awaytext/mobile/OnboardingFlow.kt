package app.awaytext.mobile

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun OnboardingFlow(onCompleted: () -> Unit) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val context = LocalContext.current
    val appPreferences = AppPreferences.getInstance(context)
    
    when (currentStep) {
        OnboardingStep.WELCOME -> {
            WelcomeIntroScreen(
                onNextClick = {
                    currentStep = OnboardingStep.SMS_PERMISSIONS
                }
            )
        }
        
        OnboardingStep.SMS_PERMISSIONS -> {
            SmsPermissionScreen(
                onPermissionGranted = {
                    currentStep = OnboardingStep.CONTACTS_PERMISSION
                },
                onSkip = {
                    currentStep = OnboardingStep.CONTACTS_PERMISSION
                }
            )
        }
        
        OnboardingStep.CONTACTS_PERMISSION -> {
            ContactsPermissionScreen(
                onPermissionGranted = {
                    currentStep = OnboardingStep.NOTIFICATION_POLICY
                },
                onSkip = {
                    currentStep = OnboardingStep.NOTIFICATION_POLICY
                }
            )
        }
        
        OnboardingStep.NOTIFICATION_POLICY -> {
            NotificationPolicyScreen(
                onPermissionGranted = {
                    currentStep = OnboardingStep.BACKGROUND_PERMISSION
                },
                onSkip = {
                    currentStep = OnboardingStep.BACKGROUND_PERMISSION
                }
            )
        }
        
        OnboardingStep.BACKGROUND_PERMISSION -> {
            BackgroundPermissionScreen(
                onPermissionGranted = {
                    currentStep = OnboardingStep.COMPLETED
                },
                onSkip = {
                    currentStep = OnboardingStep.COMPLETED
                }
            )
        }
        
        OnboardingStep.COMPLETED -> {
            OnboardingCompleteScreen(
                onFinishClick = {
                    appPreferences.setOnboardingCompleted()
                    onCompleted()
                }
            )
        }
    }
}

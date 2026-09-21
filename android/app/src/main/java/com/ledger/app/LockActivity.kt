package com.ledger.app

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Invisible activity that shows a [BiometricPrompt] for the app lock and reports the
 * outcome back to [MainActivity] via the activity result. It exists as a separate
 * activity because [BiometricPrompt] requires a [FragmentActivity], while the app's
 * Compose UI lives in a plain ComponentActivity.
 */
class LockActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        try {
            if (BiometricManager.from(this).canAuthenticate(authenticators) !=
                BiometricManager.BIOMETRIC_SUCCESS
            ) {
                // No biometric or device credential enrolled — never trap the user.
                setResult(RESULT_OK)
                finish()
                return
            }

            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        setResult(RESULT_OK)
                        finish()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                },
            )

            // DEVICE_CREDENTIAL provides the pattern/PIN/password fallback. Note that a
            // negative button must not be set when device credential is allowed.
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Ledger")
                .setSubtitle("Confirm it's you to open your ledger")
                .setAllowedAuthenticators(authenticators)
                .build()
            prompt.authenticate(info)
        } catch (e: Exception) {
            // Any prompt failure degrades to "unlocked" rather than locking the user out.
            setResult(RESULT_OK)
            finish()
        }
    }
}

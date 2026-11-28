package app.unconfirmed.game

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob, behind Google's consent form. Each launch asks the User Messaging Platform whether the
 * player has to be asked about ads (in the EEA, the UK and Switzerland, and in US states with
 * privacy laws) and shows the form if so. Ads start only once consent allows them, and until then
 * the ad slot keeps its made-up classified.
 */
object Ads {
    /** True once the Mobile Ads SDK has started and banners may be requested. */
    var ready by mutableStateOf(false)
        private set

    /** True when the player must be able to change their ad choices, from the About screen. */
    var privacyOptionsRequired by mutableStateOf(false)
        private set

    private val started = AtomicBoolean(false)

    /** Call from the activity's onCreate, on every launch. */
    fun gatherConsent(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // A form error leaves canRequestAds false where consent is needed, so no ads.
                    update(activity, consent)
                }
            },
            { update(activity, consent) },
        )
        // Consent given on an earlier launch lets ads start now, without waiting for the update.
        update(activity, consent)
    }

    /** Shows the form for changing ad choices. Offer it only while [privacyOptionsRequired]. */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            update(activity, UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun update(activity: Activity, consent: ConsentInformation) {
        privacyOptionsRequired =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!consent.canRequestAds() || !started.compareAndSet(false, true)) return
        val app = activity.applicationContext
        // Google asks for the SDK to start off the main thread. Banners requested before it
        // finishes wait for it.
        Thread { MobileAds.initialize(app) }.start()
        ready = true
    }
}

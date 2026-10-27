package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdMobManager {
    private const val TAG = "AdMobManager"

    // Ad Unit IDs requested by user
    const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-1254774552944426/9151767484"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1254774552944426/5166396802"

    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    var isInitialized = false
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        if (isInitialized) return

        // Initialize on background thread to prevent blocking main thread / WebView delays
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(context) { status ->
                    isInitialized = true
                    Log.d(TAG, "AdMob Initialized: $status")
                    mainHandler.post {
                        loadAppOpenAd(context)
                        loadInterstitialAd(context)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MobileAds on background thread", e)
            }
        }
    }

    fun loadAppOpenAd(context: Context) {
        if (isAppOpenLoading || appOpenAd != null) return
        isAppOpenLoading = true

        mainHandler.post {
            try {
                val request = AdRequest.Builder().build()
                AppOpenAd.load(
                    context,
                    APP_OPEN_AD_UNIT_ID,
                    request,
                    object : AppOpenAd.AppOpenAdLoadCallback() {
                        override fun onAdLoaded(ad: AppOpenAd) {
                            appOpenAd = ad
                            isAppOpenLoading = false
                            Log.d(TAG, "App Open Ad Loaded successfully!")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            appOpenAd = null
                            isAppOpenLoading = false
                            Log.w(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                isAppOpenLoading = false
                Log.e(TAG, "Exception loading App Open Ad", e)
            }
        }
    }

    fun showAppOpenAd(activity: Activity, onDismiss: () -> Unit = {}) {
        mainHandler.post {
            val ad = appOpenAd
            if (ad != null && !activity.isFinishing && !activity.isDestroyed) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        loadAppOpenAd(activity)
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null
                        loadAppOpenAd(activity)
                        onDismiss()
                    }
                }
                ad.show(activity)
            } else {
                loadAppOpenAd(activity)
                onDismiss()
            }
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (isInterstitialLoading || interstitialAd != null) return
        isInterstitialLoading = true

        mainHandler.post {
            try {
                val request = AdRequest.Builder().build()
                InterstitialAd.load(
                    context,
                    INTERSTITIAL_AD_UNIT_ID,
                    request,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            isInterstitialLoading = false
                            Log.d(TAG, "Interstitial Ad Loaded successfully!")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            interstitialAd = null
                            isInterstitialLoading = false
                            Log.w(TAG, "Interstitial Ad failed to load: ${loadAdError.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                isInterstitialLoading = false
                Log.e(TAG, "Exception loading Interstitial Ad", e)
            }
        }
    }

    fun showInterstitialAd(activity: Activity, onDismiss: () -> Unit = {}) {
        mainHandler.post {
            val ad = interstitialAd
            if (ad != null && !activity.isFinishing && !activity.isDestroyed) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadInterstitialAd(activity)
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        interstitialAd = null
                        loadInterstitialAd(activity)
                        onDismiss()
                    }
                }
                ad.show(activity)
            } else {
                loadInterstitialAd(activity)
                onDismiss()
            }
        }
    }

    fun isAppOpenAdReady(): Boolean = appOpenAd != null
    fun isInterstitialAdReady(): Boolean = interstitialAd != null
}

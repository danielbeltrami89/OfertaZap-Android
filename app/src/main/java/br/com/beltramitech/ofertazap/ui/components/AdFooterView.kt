package dev.beltramitech.ofertazap.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.beltramitech.ofertazap.BuildConfig
import dev.beltramitech.ofertazap.data.AnalyticsTracker
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdFooterView(
    modifier: Modifier = Modifier,
    screenName: String,
    analyticsTracker: AnalyticsTracker
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = if (BuildConfig.DEBUG) {
                    "ca-app-pub-3940256099942544/6300978111"
                } else {
                    "ca-app-pub-9920228067759661/9071550124"
                }
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adListener = object : AdListener() {
                    override fun onAdClicked() {
                        analyticsTracker.logClick("ad_banner", screenName)
                    }

                    override fun onAdImpression() {
                        analyticsTracker.logImpression("ad_banner", screenName)
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

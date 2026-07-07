package com.dailyspark.mobile.ads

object AdState {
    @Volatile
    var isAnyAdShowing: Boolean = false
    @Volatile
    var isAdLoadingToShow: Boolean = false
    fun canShowAd(): Boolean = !isAnyAdShowing && !isAdLoadingToShow

}
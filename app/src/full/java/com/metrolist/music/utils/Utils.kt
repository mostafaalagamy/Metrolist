package com.metrolist.music.utils

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.metrolist.music.MainActivity
import kotlin.time.Duration.Companion.hours

fun MainActivity.setupRemoteConfig() {
    val remoteConfig = Firebase.remoteConfig
    remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
        minimumFetchIntervalInSeconds = 6.hours.inWholeSeconds
    })
    remoteConfig.fetchAndActivate()
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                latestVersion = remoteConfig.getLong("latest_version")
            }
        }
    remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
        override fun onError(error: FirebaseRemoteConfigException) {}
        override fun onUpdate(configUpdate: ConfigUpdate) {
            remoteConfig.activate().addOnCompleteListener {
                latestVersion = remoteConfig.getLong("latest_version")
            }
        }
    })
}

fun reportException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
    throwable.printStackTrace()
}

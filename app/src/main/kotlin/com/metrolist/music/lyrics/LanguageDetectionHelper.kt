/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

object LanguageDetectionHelper {
    private val languageIdentifier: LanguageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    suspend fun identifyLanguage(text: String): String? = suspendCancellableCoroutine { cont ->
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    cont.resume(null)
                } else {
                    cont.resume(languageCode)
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Language identification failed")
                cont.resume(null)
            }
    }
}

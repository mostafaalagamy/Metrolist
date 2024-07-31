package com.metrolist.music.utils

import android.util.LruCache
import com.github.houbb.opencc4j.util.ZhConverterUtil
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.lyrics.LyricsUtils
import kotlinx.coroutines.tasks.await
import java.util.Locale

object TranslationHelper {
    private const val MAX_CACHE_SIZE = 20
    private val cache = LruCache<String, LyricsEntity>(MAX_CACHE_SIZE)

    suspend fun translate(lyrics: LyricsEntity): LyricsEntity {
        cache[lyrics.id]?.let { return it }
        val isSynced = lyrics.lyrics.startsWith("[")
        val sourceLanguage = TranslateLanguage.fromLanguageTag(
            LanguageIdentification.getClient().identifyLanguage(
                lyrics.lyrics.lines().joinToString(separator = "\n") { it.replace("\\[\\d{2}:\\d{2}.\\d{2,3}\\] *".toRegex(), "") }
            ).await()
        )
        val targetLanguage = TranslateLanguage.fromLanguageTag(
            Locale.getDefault().toLanguageTag().substring(0..1)
        )
        return if (sourceLanguage == null || targetLanguage == null || sourceLanguage == targetLanguage) {
            lyrics
        } else {
            val translator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build()
            )
            translator.downloadModelIfNeeded(
                DownloadConditions.Builder()
                    .requireWifi()
                    .build()
            ).await()
            val traditionalChinese = Locale.getDefault().toLanguageTag().replace("-Hant", "") == "zh-TW"
            lyrics.copy(
                lyrics = if (isSynced) {
                    LyricsUtils.parseLyrics(lyrics.lyrics).map {
                        val translated = translator.translate(it.text).await()
                        it.copy(
                            text = if (traditionalChinese) ZhConverterUtil.toTraditional(translated) else translated
                        )
                    }.joinToString(separator = "\n") {
                        "[%02d:%02d.%03d]${it.text}".format(it.time / 60000, (it.time / 1000) % 60, it.time % 1000)
                    }
                } else {
                    lyrics.lyrics.lines()
                        .map {
                            val translated = translator.translate(it).await()
                            if (traditionalChinese) ZhConverterUtil.toTraditional(translated) else translated
                        }
                        .joinToString(separator = "\n")
                }
            )
        }.also {
            cache.put(it.id, it)
        }
    }

    suspend fun clearModels() {
        val modelManager = RemoteModelManager.getInstance()
        val downloadedModels = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
        downloadedModels.forEach {
            modelManager.deleteDownloadedModel(it).await()
        }
    }
}
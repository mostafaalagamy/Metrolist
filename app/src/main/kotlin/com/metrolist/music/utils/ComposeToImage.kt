package com.metrolist.music.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import com.metrolist.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

object ComposeToImage {

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int? = null,
        textColor: Int? = null,
        secondaryTextColor: Int? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val cardSize = minOf(width, height) - 32
        val bitmap = createBitmap(cardSize, cardSize)
        val canvas = Canvas(bitmap)

        val defaultBackgroundColor = 0xFF121212.toInt()
        val defaultTextColor = 0xFFFFFFFF.toInt()
        val defaultSecondaryTextColor = 0xB3FFFFFF.toInt()

        val bgColor = backgroundColor ?: defaultBackgroundColor
        val mainTextColor = textColor ?: defaultTextColor
        val secondaryTxtColor = secondaryTextColor ?: defaultSecondaryTextColor

        val backgroundPaint = Paint().apply {
            color = bgColor
            isAntiAlias = true
        }
        val cornerRadius = 20f
        val backgroundRect = RectF(0f, 0f, cardSize.toFloat(), cardSize.toFloat())
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)

        var coverArtBitmap: Bitmap? = null
        if (coverArtUrl != null) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverArtUrl)
                    .size(256)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                coverArtBitmap = result.drawable?.toBitmap(256, 256, Bitmap.Config.ARGB_8888)
            } catch (_: Exception) {}
        }

        val padding = 32f
        val imageCornerRadius = 12f

        val coverArtSize = cardSize * 0.15f
        coverArtBitmap?.let {
            val rect = RectF(padding, padding, padding + coverArtSize, padding + coverArtSize)
            val path = Path().apply {
                addRoundRect(rect, imageCornerRadius, imageCornerRadius, Path.Direction.CW)
            }
            canvas.withClip(path) {
                drawBitmap(it, null, rect, null)
            }
        }

        val titlePaint = TextPaint().apply {
            color = mainTextColor
            textSize = cardSize * 0.045f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val artistPaint = TextPaint().apply {
            color = secondaryTxtColor
            textSize = cardSize * 0.035f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val textMaxWidth = cardSize - (padding * 2 + coverArtSize + 16f)
        val textStartX = padding + coverArtSize + 16f
        
        val titleAlignment = Layout.Alignment.ALIGN_NORMAL
        val artistAlignment = Layout.Alignment.ALIGN_NORMAL

        val titleLayout = StaticLayout.Builder.obtain(songTitle, 0, songTitle.length, titlePaint, textMaxWidth.toInt())
            .setAlignment(titleAlignment)
            .setMaxLines(1)
            .build()
        val artistLayout = StaticLayout.Builder.obtain(artistName, 0, artistName.length, artistPaint, textMaxWidth.toInt())
            .setAlignment(artistAlignment)
            .setMaxLines(1)
            .build()

        val imageCenter = padding + coverArtSize / 2f
        val textBlockHeight = titleLayout.height + artistLayout.height + 8f
        val textBlockY = imageCenter - textBlockHeight / 2f

        canvas.withTranslation(textStartX, textBlockY) {
            titleLayout.draw(this)
            translate(0f, titleLayout.height.toFloat() + 8f)
            artistLayout.draw(this)
        }

        val lyricsPaint = TextPaint().apply {
            color = mainTextColor
            textSize = cardSize * 0.08f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        var lyricsTextSize = cardSize * 0.08f
        var lyricsLayout: StaticLayout
        val lyricsMaxWidth = (cardSize * 0.85f).toInt()
        
        val lyricsAlignment = determineMixedTextAlignment(lyrics)

        do {
            lyricsPaint.textSize = lyricsTextSize
            lyricsLayout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, lyricsPaint, lyricsMaxWidth)
                .setAlignment(lyricsAlignment)
                .setIncludePad(false)
                .setLineSpacing(8f, 1.3f)
                .build()
            if (lyricsLayout.height > cardSize * 0.5f) {
                lyricsTextSize -= 3f
            } else break
        } while (lyricsTextSize > 24f)

        val headerHeight = padding + coverArtSize + 32f
        val footerHeight = 80f
        val availableHeight = cardSize - headerHeight - footerHeight
        
        val lyricsStartY = headerHeight + (availableHeight - lyricsLayout.height) / 2f
        val lyricsStartX = (cardSize - lyricsLayout.width) / 2f

        canvas.withTranslation(lyricsStartX, lyricsStartY) {
            lyricsLayout.draw(this)
        }

        AppLogo(context, canvas, cardSize, padding, secondaryTxtColor, bgColor)

        return@withContext bitmap
    }

    private fun AppLogo(
        context: Context,
        canvas: Canvas,
        cardSize: Int,
        padding: Float,
        secondaryTxtColor: Int,
        backgroundColor: Int
    ) {
        val logoSize = (cardSize * 0.05f).toInt()

        val rawLogo = context.getDrawable(R.drawable.small_icon)?.toBitmap(logoSize, logoSize)
        val logo = rawLogo?.let { source ->
            val colored = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvasLogo = Canvas(colored)
            val paint = Paint().apply {
                colorFilter = PorterDuffColorFilter(backgroundColor, PorterDuff.Mode.SRC_IN)
                isAntiAlias = true
            }
            canvasLogo.drawBitmap(source, 0f, 0f, paint)
            colored
        }

        val appName = context.getString(R.string.app_name)
        val appNamePaint = TextPaint().apply {
            color = secondaryTxtColor
            textSize = cardSize * 0.042f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.01f
        }

        val circleRadius = logoSize * 0.55f
        val logoX = padding + circleRadius - logoSize / 2f
        val logoY = cardSize - padding - circleRadius - logoSize / 2f
        val circleX = padding + circleRadius
        val circleY = cardSize - padding - circleRadius
        val textX = padding + circleRadius * 2 + 12f
        val textY = circleY + appNamePaint.textSize * 0.3f

        val circlePaint = Paint().apply {
            color = secondaryTxtColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(circleX, circleY, circleRadius, circlePaint)

        logo?.let {
            canvas.drawBitmap(it, logoX, logoY, null)
        }

        canvas.drawText(appName, textX, textY, appNamePaint)
    }

    private fun determineMixedTextAlignment(text: String): Layout.Alignment {
        val lines = text.split("\n")
        var arabicLines = 0
        var englishLines = 0
        
        for (line in lines) {
            if (line.trim().isEmpty()) continue
            
            val arabicChars = countArabicCharacters(line)
            val totalChars = line.replace("\\s".toRegex(), "").length
            
            if (totalChars == 0) continue
            
            val arabicRatio = arabicChars.toFloat() / totalChars
            
            when {
                arabicRatio > 0.7 -> arabicLines++
                arabicRatio < 0.3 -> englishLines++
                else -> {
                    englishLines++
                }
            }
        }
        
        return when {
            arabicLines > englishLines -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
    }

    private fun countArabicCharacters(text: String): Int {
        val arabicPattern = Pattern.compile("[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]")
        val matcher = arabicPattern.matcher(text)
        var count = 0
        while (matcher.find()) {
            count++
        }
        return count
    }

    private fun isArabicText(text: String): Boolean {
        val arabicPattern = Pattern.compile("[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFb50-\uFdff\uFE70-\uFEFF]")
        return arabicPattern.matcher(text).find()
    }

    fun saveBitmapAsFile(context: Context, bitmap: Bitmap, fileName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Metrolist")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to create new MediaStore record")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            uri
        } else {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "$fileName.png")
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                imageFile
            )
        }
    }
}

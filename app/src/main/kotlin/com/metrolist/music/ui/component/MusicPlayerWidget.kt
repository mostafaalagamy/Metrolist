import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.metrolist.music.R

class MusicPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The provideContent block is where you define your widget's UI.
        provideContent {
            GlanceMusicPlayerWidget()
        }
    }
}

@Composable
fun GlanceMusicPlayerWidget() {
    Row(
        modifier = GlanceModifier
            .background(Color.Gray),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art placeholder
        Box(
            modifier = GlanceModifier.fillMaxHeight().padding(20.dp,0.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.album),
                modifier = GlanceModifier.size(50.dp),
                contentDescription = "Album Art")
        }

        // Music info and controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.background(Color.DarkGray).fillMaxHeight().padding(10.dp,0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Metrolist",
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(
                    textAlign = TextAlign.Start,
                    fontSize = 18.sp,
                    color = ColorProvider(Color.LightGray),
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            // Player controls
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.media3_icon_thumb_down_unfilled),
                    contentDescription = "Dislike",
                    modifier = GlanceModifier.size(20.dp).defaultWeight() // Occupy available space
                )
                Image(
                    provider = ImageProvider(R.drawable.skip_previous),
                    contentDescription = "Previous",
                    modifier = GlanceModifier.size(35.dp).defaultWeight()) // Occupy available space

                Image(
                    provider = ImageProvider(R.drawable.play),
                    contentDescription = "Play",
                    modifier = GlanceModifier.size(35.dp).defaultWeight()) // Occupy available space
                 Image(
                    provider = ImageProvider(R.drawable.skip_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier.size(35.dp).defaultWeight() // Occupy available space
                )
                Image(
                    provider = ImageProvider(R.drawable.media3_icon_thumb_up_unfilled),
                    contentDescription = "Like",
                    modifier = GlanceModifier.size(20.dp).defaultWeight() // Occupy available space
                )
            }
        }
    }
}

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicPlayerWidget()
}


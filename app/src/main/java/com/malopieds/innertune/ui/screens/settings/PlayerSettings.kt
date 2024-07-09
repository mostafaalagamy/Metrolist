package com.malopieds.innertune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.malopieds.innertune.LocalPlayerAwareWindowInsets
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.AudioNormalizationKey
import com.malopieds.innertune.constants.AudioQuality
import com.malopieds.innertune.constants.AudioQualityKey
import com.malopieds.innertune.constants.PersistentQueueKey
import com.malopieds.innertune.constants.SkipSilenceKey
import com.malopieds.innertune.ui.component.EnumListPreference
import com.malopieds.innertune.ui.component.IconButton
import com.malopieds.innertune.ui.component.SwitchPreference
import com.malopieds.innertune.ui.utils.backToMain
import com.malopieds.innertune.utils.rememberEnumPreference
import com.malopieds.innertune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(key = AudioQualityKey, defaultValue = AudioQuality.AUTO)
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(key = PersistentQueueKey, defaultValue = true)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(key = SkipSilenceKey, defaultValue = false)
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(key = AudioNormalizationKey, defaultValue = true)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        EnumListPreference(
            title = { Text(stringResource(R.string.audio_quality)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            selectedValue = audioQuality,
            onValueSelected = onAudioQualityChange,
            valueText = {
                when (it) {
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                }
            },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.persistent_queue)) },
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = persistentQueue,
            onCheckedChange = onPersistentQueueChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.skip_silence)) },
            icon = { Icon(painterResource(R.drawable.skip_next), null) },
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.audio_normalization)) },
            icon = { Icon(painterResource(R.drawable.volume_up), null) },
            checked = audioNormalization,
            onCheckedChange = onAudioNormalizationChange,
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

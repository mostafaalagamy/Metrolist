/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AiProviderKey
import com.metrolist.music.constants.AutoTranslateLyricsKey
import com.metrolist.music.constants.AutoTranslateLyricsMismatchKey
import com.metrolist.music.constants.LanguageCodeToName
import com.metrolist.music.constants.OpenRouterApiKey
import com.metrolist.music.constants.OpenRouterBaseUrlKey
import com.metrolist.music.constants.OpenRouterModelKey
import com.metrolist.music.constants.TranslateLanguageKey
import com.metrolist.music.constants.TranslateModeKey
import com.metrolist.music.ui.component.EditTextPreference
import com.metrolist.music.ui.component.ListPreference
import com.metrolist.music.ui.component.SwitchPreference
import com.metrolist.music.utils.rememberPreference
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    var openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    var openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
    var openRouterModel by rememberPreference(OpenRouterModelKey, "mistralai/mistral-small-3.1-24b-instruct:free")
    var autoTranslateLyrics by rememberPreference(AutoTranslateLyricsKey, false)
    var autoTranslateLyricsMismatch by rememberPreference(AutoTranslateLyricsMismatchKey, false)
    var translateLanguage by rememberPreference(TranslateLanguageKey, "en")

    val aiProviders = mapOf(
        "OpenRouter" to "https://openrouter.ai/api/v1/chat/completions",
        "ChatGPT" to "https://api.openai.com/v1/chat/completions",
        "Perplexity" to "https://api.perplexity.ai/chat/completions",
        "Claude" to "https://api.anthropic.com/v1/messages",
        "Gemini" to "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        "Grok" to "https://api.x.ai/v1/chat/completions",
        "Custom" to ""
    )

    val models = listOf(
        "google/gemini-flash-1.5",
        "openai/gpt-3.5-turbo",
        "anthropic/claude-3-haiku",
        "meta-llama/llama-3-8b-instruct"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Lyrics Translation") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.navigateUp() }) {
                        androidx.compose.material3.Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Setup Guide", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val annotatedString = buildAnnotatedString {
                        append("1. Select your Provider (e.g., OpenRouter, ChatGPT) or 'Custom'.\n")
                        append("2. Enter your API Key.\n")
                        append("3. If 'Custom', enter the Base URL provided by your service.\n\n")
                        
                        append("Need an API Key? Try ")
                        pushStringAnnotation(tag = "URL", annotation = "https://openrouter.ai")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append("OpenRouter.ai")
                        }
                        pop()
                        append(" for access to many models.")
                    }
                    
                    val uriHandler = LocalUriHandler.current
                    
                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                ListPreference(
                    title = { Text("Provider") },
                    selectedValue = aiProvider,
                    values = aiProviders.keys.toList(),
                    valueText = { it },
                    onValueSelected = { 
                        aiProvider = it
                        if (it != "Custom") {
                            openRouterBaseUrl = aiProviders[it] ?: ""
                        } else {
                            openRouterBaseUrl = ""
                        }
                        if (it == "OpenRouter") {
                            openRouterModel = "mistralai/mistral-small-3.1-24b-instruct:free"
                        } else {
                            openRouterModel = ""
                        }
                    },
                    icon = { Icon(painterResource(R.drawable.explore_outlined), null) }
                )

                if (aiProvider == "Custom") {
                    EditTextPreference(
                        title = { Text("Base URL") },
                        value = openRouterBaseUrl,
                        onValueChange = { openRouterBaseUrl = it },
                        icon = { Icon(painterResource(R.drawable.link), null) }
                    )
                }

                EditTextPreference(
                    title = { Text("API Key") },
                    value = openRouterApiKey,
                    onValueChange = { openRouterApiKey = it },
                    icon = { Icon(painterResource(R.drawable.key), null) }
                )

                EditTextPreference(
                    title = { Text("Model") },
                    value = openRouterModel,
                    onValueChange = { openRouterModel = it },
                    icon = { Icon(painterResource(R.drawable.discover_tune), null) }
                )

                SwitchPreference(
                    title = { Text("Auto translate all songs") },
                    checked = autoTranslateLyrics,
                    onCheckedChange = { autoTranslateLyrics = it },
                    icon = { Icon(painterResource(R.drawable.translate), null) }
                )

                if (autoTranslateLyrics) {
                    SwitchPreference(
                        modifier = Modifier.padding(start = 24.dp),
                        title = { Text("Translate only on language mismatch") },
                        description = "Skip translation if lyrics identify as your system language",
                        checked = autoTranslateLyricsMismatch,
                        onCheckedChange = { autoTranslateLyricsMismatch = it }
                    )

                    var translateMode by rememberPreference(TranslateModeKey, "Literal")
                    ListPreference(
                        modifier = Modifier.padding(start = 24.dp),
                        title = { Text("Translation Mode") },
                        selectedValue = translateMode,
                        values = listOf("Literal", "Transcribed"),
                        valueText = { 
                            when(it) {
                                "Literal" -> "Original + Translation"
                                "Transcribed" -> "Original + Transcribed"
                                else -> it
                            }
                        },
                        onValueSelected = { translateMode = it }
                    )

                    if (!autoTranslateLyricsMismatch) {
                        ListPreference(
                            modifier = Modifier.padding(start = 24.dp),
                            title = { Text("Target Language") },
                            selectedValue = translateLanguage,
                            values = LanguageCodeToName.keys.sortedBy { LanguageCodeToName[it] },
                            valueText = { LanguageCodeToName[it] ?: it },
                            onValueSelected = { translateLanguage = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

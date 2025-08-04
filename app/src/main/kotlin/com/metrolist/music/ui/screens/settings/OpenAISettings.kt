package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAISettings(
    onBackClick: () -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onSaveApiKey: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenAI Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🌟 OpenAI GPT-4o Integration",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "احصل على أفضل ترجمة للكلمات باستخدام الذكاء الاصطناعي المتقدم",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // API Key Input
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "API Key Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text("OpenAI API Key") },
                        placeholder = { Text("sk-...") },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isApiKeyVisible) "Hide API Key" else "Show API Key"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = onSaveApiKey,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = apiKey.isNotBlank()
                    ) {
                        Text("حفظ API Key")
                    }
                }
            }

            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📖 خطوات الحصول على API Key",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { showInstructions = !showInstructions }
                        ) {
                            Text(if (showInstructions) "إخفاء" else "عرض")
                        }
                    }
                    
                    if (showInstructions) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Step 1
                        StepCard(
                            stepNumber = "1",
                            title = "إنشاء حساب OpenAI",
                            description = "اذهب إلى platform.openai.com وأنشئ حساب جديد أو سجل دخولك",
                            actionText = "فتح الموقع",
                            onActionClick = { 
                                uriHandler.openUri("https://platform.openai.com/") 
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Step 2
                        StepCard(
                            stepNumber = "2",
                            title = "الذهاب إلى API Keys",
                            description = "في لوحة التحكم، اضغط على 'API Keys' من القائمة الجانبية"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Step 3
                        StepCard(
                            stepNumber = "3",
                            title = "إنشاء مفتاح جديد",
                            description = "اضغط على 'Create new secret key' واختر اسم للمفتاح"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Step 4
                        StepCard(
                            stepNumber = "4",
                            title = "نسخ المفتاح",
                            description = "انسخ المفتاح فوراً - لن تتمكن من رؤيته مرة أخرى!"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Step 5
                        StepCard(
                            stepNumber = "5",
                            title = "لصق المفتاح هنا",
                            description = "الصق المفتاح في الحقل أعلاه واضغط 'حفظ'"
                        )
                    }
                }
            }

            // Pricing Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💰 معلومات التكلفة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "• النموذج: GPT-4o (الأحدث والأقوى)\n" +
                               "• التكلفة: ~0.01-0.02$ لكل أغنية\n" +
                               "• رصيد مجاني: 5$ عند التسجيل\n" +
                               "• يكفي لترجمة 250-500 أغنية مجاناً!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Benefits
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✨ المميزات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "🎵 ترجمة متخصصة للكلمات الموسيقية\n" +
                               "🎭 حفظ المشاعر والمعنى الأصلي\n" +
                               "🌍 تكيف ثقافي ذكي\n" +
                               "⚡ جودة 25% أفضل من الترجمة التقليدية\n" +
                               "🎶 ترجمات قابلة للغناء وشاعرية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Warning
            if (apiKey.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚠️ تنبيه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "بدون API Key، ستعمل الترجمة باستخدام الخدمات الأخرى فقط (Google Translate). للحصول على أفضل جودة، يرجى إضافة OpenAI API Key.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepCard(
    stepNumber: String,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step number
            Card(
                modifier = Modifier.size(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (actionText != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onActionClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(actionText)
                    }
                }
            }
        }
    }
}
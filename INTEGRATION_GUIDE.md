# دليل التكامل - VerticalFastScroller

## خطوات التكامل مع مشروعك

### 1. نسخ الملفات
```bash
# انسخ VerticalFastScroller.kt إلى مشروعك
cp VerticalFastScroller.kt /path/to/your/project/app/src/main/java/com/yourpackage/ui/component/
```

### 2. تحديث Package Name
افتح `VerticalFastScroller.kt` وغيّر السطر الأول:
```kotlin
// من
package com.yourpackage.ui.component

// إلى
package com.yourappname.ui.component
```

### 3. إضافة Dependencies (إذا لم تكن موجودة)
في ملف `build.gradle.kts` للتطبيق:
```kotlin
dependencies {
    // Compose BOM
    implementation platform('androidx.compose:compose-bom:2024.02.00')
    
    // Compose UI
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3'
    
    // Foundation (للـ LazyColumn)
    implementation 'androidx.compose.foundation:foundation'
    
    // Activity Compose
    implementation 'androidx.activity:activity-compose:1.8.2'
}
```

### 4. الاستخدام في شاشاتك

#### مثال: قائمة تشغيل
```kotlin
@Composable
fun PlaylistScreen(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    VerticalFastScroller(
        listState = listState,
        topContentPadding = 16.dp,
        endContentPadding = 0.dp
    ) {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(songs) { song ->
                SongItem(
                    song = song,
                    onClick = { /* تشغيل الأغنية */ }
                )
            }
        }
    }
}
```

#### مثال: قائمة الألبومات
```kotlin
@Composable
fun AlbumsScreen(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    val listState = rememberLazyListState()
    
    VerticalFastScroller(
        listState = listState,
        thumbColor = MaterialTheme.colorScheme.primary
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(albums) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}
```

### 5. التخصيص المتقدم

#### تغيير شكل المؤشر
```kotlin
// في VerticalFastScroller.kt، يمكنك تعديل:
private val ThumbLength = 60.dp        // طول أكبر
private val ThumbThickness = 12.dp     // سماكة أكبر
private val ThumbShape = RoundedCornerShape(6.dp) // زوايا مربعة أكثر
```

#### ألوان مخصصة
```kotlin
VerticalFastScroller(
    listState = listState,
    thumbColor = Color(0xFF6200EA), // لون بنفسجي
    // أو
    thumbColor = MaterialTheme.colorScheme.secondary
) {
    // LazyColumn content
}
```

### 6. أمثلة لحالات استخدام مختلفة

#### مع Search Bar
```kotlin
@Composable
fun SearchablePlaylistScreen(
    songs: List<Song>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val filteredSongs = songs.filter { 
        it.title.contains(searchQuery, ignoreCase = true) 
    }
    
    Column {
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )
        
        VerticalFastScroller(
            listState = listState
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSongs) { song ->
                    SongItem(song = song)
                }
            }
        }
    }
}
```

#### مع Pull to Refresh
```kotlin
@Composable
fun RefreshablePlaylistScreen(
    songs: List<Song>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        VerticalFastScroller(
            listState = listState
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(songs) { song ->
                    SongItem(song = song)
                }
            }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
```

### 7. نصائح للأداء

1. **استخدم `key` في items**:
```kotlin
items(songs, key = { it.id }) { song ->
    SongItem(song = song)
}
```

2. **تجنب العمليات الثقيلة في item**:
```kotlin
// سيء ❌
items(songs) { song ->
    val bitmap = remember { loadBitmap(song.coverUrl) }
    SongItem(song = song, cover = bitmap)
}

// جيد ✅
items(songs) { song ->
    SongItem(
        song = song,
        onLoadCover = { loadBitmapAsync(song.coverUrl) }
    )
}
```

### 8. استكشاف الأخطاء

#### المؤشر لا يظهر
- تأكد من أن القائمة تحتوي على عناصر أكثر من حجم الشاشة
- تحقق من أن `listState` هو نفسه في `VerticalFastScroller` و `LazyColumn`

#### المؤشر يظهر في مكان خاطئ
- تأكد من إعداد `topContentPadding` و `endContentPadding` بشكل صحيح

#### الأداء بطيء
- استخدم `key` في `items()`
- تجنب العمليات الثقيلة في `item` composables

### 9. الدعم والمساعدة

إذا واجهت أي مشاكل:
1. تحقق من أن جميع imports موجودة
2. تأكد من أن package name صحيح
3. راجع الأمثلة في `ExampleUsage.kt`
4. اقرأ التوثيق في `README_VerticalScrollIndicator.md`
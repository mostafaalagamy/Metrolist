#!/bin/bash

# Script to add missing Icons imports to Kotlin files

# Function to add Icons imports to a file
add_icons_imports() {
    local file="$1"
    echo "Processing $file"
    
    # Check if file already has Icons import
    if grep -q "import androidx.compose.material.icons.Icons" "$file"; then
        echo "  Already has Icons import, skipping"
        return
    fi
    
    # Find the last import statement
    local last_import_line=$(grep -n "^import " "$file" | tail -1 | cut -d: -f1)
    
    if [ -z "$last_import_line" ]; then
        echo "  No import statements found, skipping"
        return
    fi
    
    # Add the Icons import after the last import
    sed -i "${last_import_line}a\\
import androidx.compose.material.icons.Icons\\
import androidx.compose.material.icons.filled.*" "$file"
    
    echo "  Added Icons imports"
}

# List of files that need Icons imports
files=(
    "app/src/main/kotlin/com/metrolist/music/MainActivity.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/ChipsRow.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/CreatePlaylistDialog.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/Dialog.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/EmptyPlaceholder.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/GridMenu.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/HideOnScrollFAB.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/IconButton.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/Items.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/Library.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/LyricsImageCard.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/NavigationTile.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/NavigationTitle.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/PlayingIndicator.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/Preference.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/component/SortHeader.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/menu/AddToPlaylistDialog.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/menu/AlbumMenu.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/menu/ImportPlaylistDialog.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/menu/PlaylistMenu.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/menu/SongMenu.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/menu/YouTubeAlbumMenu.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/menu/YouTubeSongMenu.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/player/Player.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/player/Queue.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/screens/Screens.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/screens/settings/SettingsScreen.kt"
    "app/src/main/kotlin/com/metrolist/music/ui/utils/ShowMediaInfo.kt"
)

# Process each file
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        add_icons_imports "$file"
    else
        echo "File not found: $file"
    fi
done

echo "Done!"
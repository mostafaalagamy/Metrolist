package com.metrolist.music.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.width(12.dp))

        chips.forEach { (value, label) ->
            FilterChip(
                label = { Text(label) },
                selected = currentValue == value,
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.background),
                onClick = { onValueUpdate(value) },
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}

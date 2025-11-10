package com.metrolist.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> EnumDialog(
    onDismiss: () -> Unit,
    title: String,
    values: List<T>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    valueText: @Composable (T) -> String
) {
    ListDialog(
        onDismiss = onDismiss,
    ) {
        item {
            Text(
                text = title,
                modifier = Modifier.padding(16.dp)
            )
        }
        values.forEach { value ->
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onValueSelected(value) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = value == selectedValue,
                        onClick = { onValueSelected(value) }
                    )
                    Text(
                        text = valueText(value),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

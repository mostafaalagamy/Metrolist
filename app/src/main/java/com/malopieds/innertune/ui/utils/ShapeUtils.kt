package com.malopieds.innertune.ui.utils

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.dp

fun CornerBasedShape.top(): CornerBasedShape = copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

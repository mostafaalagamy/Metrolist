/*
 * Copyright (C) 2026 Metrolist Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.metrolist.music.ui.utils

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.dp

fun CornerBasedShape.top(): CornerBasedShape =
    copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

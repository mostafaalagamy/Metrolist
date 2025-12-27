/*
 * Copyright (C) 2026 Metrolist Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.metrolist.music.models

import com.metrolist.innertube.models.YTItem
import com.metrolist.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)

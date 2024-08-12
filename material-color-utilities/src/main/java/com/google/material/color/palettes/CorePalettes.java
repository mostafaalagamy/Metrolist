/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.material.color.palettes;

/**
 * Comprises foundational palettes to build a color scheme.
 *
 * <p>Generated from a source color, these palettes will then be part of a [DynamicScheme] together
 * with appearance preferences.
 */
public final class CorePalettes {
    public TonalPalette primary;
    public TonalPalette secondary;
    public TonalPalette tertiary;
    public TonalPalette neutral;
    public TonalPalette neutralVariant;

    /**
     * Creates a new CorePalettes.
     */
    public CorePalettes(
            TonalPalette primary,
            TonalPalette secondary,
            TonalPalette tertiary,
            TonalPalette neutral,
            TonalPalette neutralVariant) {
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
        this.neutral = neutral;
        this.neutralVariant = neutralVariant;
    }
}

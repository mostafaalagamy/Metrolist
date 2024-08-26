/*
 * Copyright 2021 Google LLC
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

package com.google.material.color.score;

import com.google.material.color.hct.Cam16;
import com.google.material.color.hct.Hct;
import com.google.material.color.utils.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Given a large set of colors, remove colors that are unsuitable for a UI theme, and rank the rest
 * based on suitability.
 *
 * <p>Enables use of a high cluster count for image quantization, thus ensuring colors aren't
 * muddied, while curating the high cluster count to a much smaller number of appropriate choices.
 */
public final class Score {
    private static final double TARGET_CHROMA = 48.; // A1 Chroma
    private static final double WEIGHT_PROPORTION = 0.7;
    private static final double WEIGHT_CHROMA_ABOVE = 0.3;
    private static final double WEIGHT_CHROMA_BELOW = 0.1;
    private static final double CUTOFF_CHROMA = 5.;
    private static final double CUTOFF_EXCITED_PROPORTION = 0.01;

    private Score() {
    }

    public static List<Integer> score(Map<Integer, Integer> colorsToPopulation) {
        // Fallback color is Google Blue.
        return score(colorsToPopulation, 4, 0xff4285f4, true);
    }

    public static List<Integer> score(Map<Integer, Integer> colorsToPopulation, int desired) {
        return score(colorsToPopulation, desired, 0xff4285f4, true);
    }

    public static List<Integer> score(
            Map<Integer, Integer> colorsToPopulation, int desired, int fallbackColorArgb) {
        return score(colorsToPopulation, desired, fallbackColorArgb, true);
    }

    /**
     * Given a map with keys of colors and values of how often the color appears, rank the colors
     * based on suitability for being used for a UI theme.
     *
     * @param colorsToPopulation map with keys of colors and values of how often the color appears,
     *                           usually from a source image.
     * @param desired            max count of colors to be returned in the list.
     * @param fallbackColorArgb  color to be returned if no other options available.
     * @param filter             whether to filter out undesireable combinations.
     * @return Colors sorted by suitability for a UI theme. The most suitable color is the first item,
     * the least suitable is the last. There will always be at least one color returned. If all
     * the input colors were not suitable for a theme, a default fallback color will be provided,
     * Google Blue.
     */
    public static List<Integer> score(
            Map<Integer, Integer> colorsToPopulation,
            int desired,
            int fallbackColorArgb,
            boolean filter) {

        // Get the HCT color for each Argb value, while finding the per hue count and
        // total count.
        List<Hct> colorsHct = new ArrayList<>();
        int[] huePopulation = new int[360];
        double populationSum = 0.;
        for (Map.Entry<Integer, Integer> entry : colorsToPopulation.entrySet()) {
            Hct hct = Hct.fromInt(entry.getKey());
            colorsHct.add(hct);
            int hue = (int) Math.floor(hct.getHue());
            huePopulation[hue] += entry.getValue();
            populationSum += entry.getValue();
        }

        // Hues with more usage in neighboring 30 degree slice get a larger number.
        double[] hueExcitedProportions = new double[360];
        for (int hue = 0; hue < 360; hue++) {
            double proportion = huePopulation[hue] / populationSum;
            for (int i = hue - 14; i < hue + 16; i++) {
                int neighborHue = MathUtils.sanitizeDegreesInt(i);
                hueExcitedProportions[neighborHue] += proportion;
            }
        }

        // Scores each HCT color based on usage and chroma, while optionally
        // filtering out values that do not have enough chroma or usage.
        List<ScoredHCT> scoredHcts = new ArrayList<>();
        for (Hct hct : colorsHct) {
            int hue = MathUtils.sanitizeDegreesInt((int) Math.round(hct.getHue()));
            double proportion = hueExcitedProportions[hue];
            if (filter && (hct.getChroma() < CUTOFF_CHROMA || proportion <= CUTOFF_EXCITED_PROPORTION)) {
                continue;
            }

            double proportionScore = proportion * 100.0 * WEIGHT_PROPORTION;
            double chromaWeight =
                    hct.getChroma() < TARGET_CHROMA ? WEIGHT_CHROMA_BELOW : WEIGHT_CHROMA_ABOVE;
            double chromaScore = (hct.getChroma() - TARGET_CHROMA) * chromaWeight;
            double score = proportionScore + chromaScore;
            scoredHcts.add(new ScoredHCT(hct, score));
        }
        // Sorted so that colors with higher scores come first.
        Collections.sort(scoredHcts, new ScoredComparator());

        // Iterates through potential hue differences in degrees in order to select
        // the colors with the largest distribution of hues possible. Starting at
        // 90 degrees(maximum difference for 4 colors) then decreasing down to a
        // 15 degree minimum.
        List<Hct> chosenColors = new ArrayList<>();
        for (int differenceDegrees = 90; differenceDegrees >= 15; differenceDegrees--) {
            chosenColors.clear();
            for (ScoredHCT entry : scoredHcts) {
                Hct hct = entry.hct;
                boolean hasDuplicateHue = false;
                for (Hct chosenHct : chosenColors) {
                    if (MathUtils.differenceDegrees(hct.getHue(), chosenHct.getHue()) < differenceDegrees) {
                        hasDuplicateHue = true;
                        break;
                    }
                }
                if (!hasDuplicateHue) {
                    chosenColors.add(hct);
                }
                if (chosenColors.size() >= desired) {
                    break;
                }
            }
            if (chosenColors.size() >= desired) {
                break;
            }
        }
        List<Integer> colors = new ArrayList<>();
        if (chosenColors.isEmpty()) {
            colors.add(fallbackColorArgb);
        }
        for (Hct chosenHct : chosenColors) {
            colors.add(chosenHct.toInt());
        }
        return colors;
    }

    public static List<Integer> order(Map<Integer, Integer> colorsToPopulation) {
        // Determine the total count of all colors.
        double populationSum = 0.;
        for (Map.Entry<Integer, Integer> entry : colorsToPopulation.entrySet()) {
            populationSum += entry.getValue();
        }

        // Turn the count of each color into a proportion by dividing by the total count.
        // Also, fill a cache of CAM16 colors representing each color, and
        // record the proportion of colors for each CAM16 hue.
        Map<Integer, Cam16> colorsToCam = new HashMap<>();
        double[] hueProportions = new double[361];
        for (Map.Entry<Integer, Integer> entry : colorsToPopulation.entrySet()) {
            int color = entry.getKey();
            double population = entry.getValue();
            double proportion = population / populationSum;

            Cam16 cam = Cam16.fromInt(color);
            colorsToCam.put(color, cam);

            int hue = (int) Math.round(cam.getHue());
            hueProportions[hue] += proportion;
        }

        // Determine the proportion of the colors around each color, by summing the
        // proportions around each color's hue.
        Map<Integer, Double> colorsToExcitedProportion = new HashMap<>();
        for (Map.Entry<Integer, Cam16> entry : colorsToCam.entrySet()) {
            int color = entry.getKey();
            Cam16 cam = entry.getValue();
            int hue = (int) Math.round(cam.getHue());

            double excitedProportion = 0.;
            for (int j = (hue - 15); j < (hue + 15); j++) {
                int neighborHue = MathUtils.sanitizeDegreesInt(j);
                excitedProportion += hueProportions[neighborHue];
            }

            colorsToExcitedProportion.put(color, excitedProportion);
        }

        // Create a list of ScoredHCT objects with scores based on the proportion.
        List<ScoredHCT> scoredColors = new ArrayList<>();
        for (Map.Entry<Integer, Cam16> entry : colorsToCam.entrySet()) {
            int color = entry.getKey();
            double proportion = colorsToExcitedProportion.get(color);
            double proportionScore = proportion * 100.0 * WEIGHT_PROPORTION;

            scoredColors.add(new ScoredHCT(Hct.fromInt(color), proportionScore));
        }

        // Sort the list using the new ScoredComparator.
        scoredColors.sort(new ScoredComparator());

        // Extract the sorted colors, ensuring no duplicate hues are chosen.
        List<Integer> colorsByScoreDescending = new ArrayList<>();
        for (ScoredHCT scoredColor : scoredColors) {
            Hct hct = scoredColor.hct;
            int color = hct.toInt();
            Cam16 cam = colorsToCam.get(color);
            boolean duplicateHue = false;

            for (Integer alreadyChosenColor : colorsByScoreDescending) {
                Cam16 alreadyChosenCam = colorsToCam.get(alreadyChosenColor);
                if (MathUtils.differenceDegrees(cam.getHue(), alreadyChosenCam.getHue()) < 15) {
                    duplicateHue = true;
                    break;
                }
            }

            if (duplicateHue) {
                continue;
            }
            colorsByScoreDescending.add(color);
        }

        return colorsByScoreDescending;
    }


    private static class ScoredHCT {
        public final Hct hct;
        public final double score;

        public ScoredHCT(Hct hct, double score) {
            this.hct = hct;
            this.score = score;
        }
    }

    private static class ScoredComparator implements Comparator<ScoredHCT> {
        public ScoredComparator() {
        }

        @Override
        public int compare(ScoredHCT entry1, ScoredHCT entry2) {
            return Double.compare(entry2.score, entry1.score);
        }
    }
}
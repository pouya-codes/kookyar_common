package com.kookyar.common;

public final class QuarterToneUtil {

    private static final String[] NOTE_NAMES_FA = {
            "لا", "لا کرن", "سی بمل", "سی", "دو", "دو کرن", "ر", "ر کرن",
            "می", "می کرن", "فا", "فا کرن", "سل", "سل کرن", "لا", "لا کرن",
            "سی بمل", "سی", "دو", "دو کرن", "ر", "ر کرن", "می", "می کرن"
    };

    private static final String[] NOTE_NAMES_EN = {
            "A", "A\u00BD", "Bb", "B", "C", "C\u00BD", "D", "D\u00BD",
            "E", "E\u00BD", "F", "F\u00BD", "G", "G\u00BD", "A", "A\u00BD",
            "Bb", "B", "C", "C\u00BD", "D", "D\u00BD", "E", "E\u00BD"
    };

    private static final String[] OCTAVE_NAMES_FA = {
            "", "اول", "دوم", "سوم", "چهارم", "پنجم", "ششم", "هفتم", "هشتم"
    };

    private QuarterToneUtil() {
    }

    public static float midiToFrequency(float midi, int aReference) {
        return (float) (aReference * Math.pow(2, (midi - 69) / 12.0));
    }

    public static int nearestQuarterTone(float frequency, int aReference) {
        if (frequency <= 0 || aReference <= 0) {
            return 0;
        }
        double steps = 24.0 * (Math.log(frequency / aReference) / Math.log(2));
        return (int) Math.round(steps);
    }

    public static float getDeviationCents(float frequency, int aReference) {
        int qt = nearestQuarterTone(frequency, aReference);
        float target = quarterToneToFrequency(qt, aReference);
        if (target <= 0) {
            return 0;
        }
        return (float) (1200.0 * Math.log(frequency / target) / Math.log(2));
    }

    public static String getNoteName(int qtIndex) {
        int note = mod24(qtIndex);
        return NOTE_NAMES_FA[note];
    }

    public static String getNoteNameWestern(int qtIndex) {
        int note = mod24(qtIndex);
        return NOTE_NAMES_EN[note];
    }

    public static String getOctaveNameFa(int qtIndex) {
        int octave = 4 + floorDiv(qtIndex, 24);
        if (octave >= 0 && octave < OCTAVE_NAMES_FA.length) {
            return "اکتاو " + OCTAVE_NAMES_FA[octave];
        }
        return "اکتاو " + octave;
    }

    private static float quarterToneToFrequency(int qtIndex, int aReference) {
        return (float) (aReference * Math.pow(2, qtIndex / 24.0));
    }

    private static int mod24(int value) {
        int result = value % 24;
        return result < 0 ? result + 24 : result;
    }

    private static int floorDiv(int value, int divisor) {
        int result = value / divisor;
        if (value < 0 && value % divisor != 0) {
            result--;
        }
        return result;
    }
}

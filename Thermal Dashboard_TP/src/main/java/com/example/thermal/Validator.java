package com.example.thermal;

import java.util.Set;

import org.opencv.core.Mat;

public class Validator {

    // Known valid panel IDs (example dataset)
    private static final Set<String> VALID_PANELS = Set.of(
            "panel_1","panel_2","panel_3","panel_4","panel_5",
            "panel_6","panel_7","panel_8","panel_9","panel_10",
            "panel_81","panel_82","panel_83","panel_84","panel_85" );

    public static boolean isValidPanelId(String panelId) {
        if (panelId == null || panelId.trim().isEmpty())
            return false;
        String cleanId = panelId.trim().toLowerCase();
        // Regex Panel-[A-Z]\d+ OR known list
        return VALID_PANELS.contains(cleanId) || cleanId.matches("panel_[1-85]\\d+");
    }

    public static String getPanelIdError(String panelId) {
        return "Invalid Panel ID '" + panelId + "'. Use format 'panel_No.' or known IDs like panel_1, panel_2.";
    }

    public static ValidationResult validateImage(Mat image, String filename) {
        if (image == null || image.empty()) {
            return ValidationResult.invalid("Image unreadable or blank.");
        }
        return ValidationResult.valid();
    }
}

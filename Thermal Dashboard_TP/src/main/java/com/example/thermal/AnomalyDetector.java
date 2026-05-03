package com.example.thermal;

import java.time.LocalDateTime;
import java.util.List;

import org.opencv.core.Mat;

public class AnomalyDetector {

    /**
     * Legacy threshold-based detection (deprecated)
     */
    @Deprecated
    public static Result detect(ThermalFeatures features, String panelId) {
        String severity;
        if (features.getMaxTemperature() > 80 || features.getHotspotIntensity() > 65) {
            severity = "CRITICAL";
        } else if (features.getMaxTemperature() > 60) {
            severity = "WARNING";
        } else {
            severity = "NORMAL";
        }

        String status = "NORMAL".equals(severity) ? "NORMAL" : "FAULT";

        double anomalyScore = Math.max(0, Math.min(1,
                (features.getMaxTemperature() - 40) / 60.0 * 0.6 +
                        features.getHotspotIntensity() / 300.0 * 0.4));

        Result result = new Result(status, features.getMaxTemperature(), LocalDateTime.now(), panelId, anomalyScore);
        result.setSeverity(severity);
        result.setMaxTemperature(features.getMaxTemperature());
        result.setAvgTemperature(features.getAvgTemperature());
        result.setHotspotIntensity(features.getHotspotIntensity());
        result.setThermalGradient(features.getThermalGradient());
        result.setStdTemperature(features.getStdTemperature());

        return result;
    }

    /**
     * Hybrid anomaly detection: annotations + dynamic hotspots for electrical
     * panels
     */
    public static Result detect(Mat image, String filename, String panelId) {
        // Dynamic hotspots
        List<HotspotRegion> hotspots = ImageProcessor.detectHotspotsElectrical(image);

        // Thermal features
        ThermalFeatures features = ImageProcessor.extractFeatures(image);

        // Unsupervised - no annotations

        // Status/severity
        boolean hasFault = !hotspots.isEmpty();
        String status = hasFault ? "FAULT" : "NORMAL";
        // Severity based on mapped temperatures
        String severity = "NORMAL";
        if (features.getMaxTemperature() > 80
                || (!hotspots.isEmpty() && hotspots.stream().anyMatch(h -> h.getAnomalyScore() > 0.5))) {
            severity = "CRITICAL";
        } else if (features.getMaxTemperature() > 60 || !hotspots.isEmpty()) {
            severity = "WARNING";
        }

        // Anomaly score: avg hotspot score or feature-based
        double score = 0.0;
        if (!hotspots.isEmpty()) {
            score = hotspots.stream().mapToDouble(HotspotRegion::getAnomalyScore).average().orElse(0.0);
        } else if (features.getMaxTemperature() > 60) {
            score = (features.getMaxTemperature() - 40) / 60.0;
        }

        Result result = new Result(status, features.getMaxTemperature(), LocalDateTime.now(), panelId, score);
        result.setSeverity(severity);
        result.setFilename(filename);
        result.setAnomalousRegions(hotspots);

        result.setFaultCount(hotspots.size());
        result.setPrimaryFaultType(
                hasFault ? hotspots.stream().max((a, b) -> Double.compare(a.getAnomalyScore(), b.getAnomalyScore()))
                        .map(HotspotRegion::getType).orElse("HOTSPOT") : "None");

        // Thermal features
        result.setMaxTemperature(features.getMaxTemperature());
        result.setAvgTemperature(features.getAvgTemperature());
        result.setHotspotIntensity(features.getHotspotIntensity());
        result.setThermalGradient(features.getThermalGradient());
        result.setStdTemperature(features.getStdTemperature());

        return result;
    }
}

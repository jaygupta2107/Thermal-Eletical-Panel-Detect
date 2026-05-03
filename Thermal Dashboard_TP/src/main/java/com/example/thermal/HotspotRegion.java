package com.example.thermal;

import org.opencv.core.Rect;

/**
 * Represents a detected anomalous/hotspot region in electrical panels.
 * Includes bounding box, thermal properties, anomaly score, and fault type.
 */
public class HotspotRegion {
    private Rect boundingBox;
    private double avgTemperature;
    private double hotspotIntensity;
    private double anomalyScore;
    private String type; // "HOTSPOT", "LOOSE_CONNECTION", "BREAKER_OVERHEAT", "ANOMALY"

    public HotspotRegion() {
    }

    public HotspotRegion(Rect boundingBox, double avgTemperature, double hotspotIntensity,
            double anomalyScore, String type) {
        this.boundingBox = boundingBox;
        this.avgTemperature = avgTemperature;
        this.hotspotIntensity = hotspotIntensity;
        this.anomalyScore = anomalyScore;
        this.type = type;
    }

    // Getters and Setters
    public Rect getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(Rect boundingBox) {
        this.boundingBox = boundingBox;
    }

    public double getAvgTemperature() {
        return avgTemperature;
    }

    public void setAvgTemperature(double avgTemperature) {
        this.avgTemperature = avgTemperature;
    }

    public double getHotspotIntensity() {
        return hotspotIntensity;
    }

    public void setHotspotIntensity(double hotspotIntensity) {
        this.hotspotIntensity = hotspotIntensity;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("HotspotRegion{type=%s, bbox=%s, temp=%.1f°C, score=%.2f}",
                type, boundingBox, avgTemperature, anomalyScore);
    }
}

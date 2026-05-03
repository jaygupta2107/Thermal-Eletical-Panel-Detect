package com.example.thermal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Result {
    private String status; // NORMAL / FAULT
    private double temperature;
    private LocalDateTime time;
    private String panelId;
    private double anomalyScore;
    private String severity;
    private double maxTemperature;
    private double avgTemperature;
    private double hotspotIntensity;
    private double thermalGradient;
    private double stdTemperature;
    private String filename;

    /** Transient — holds the OpenCV-generated heatmap for third-party images. Not persisted. */
    private transient javafx.scene.image.Image generatedHeatmap;

    private int faultCount;
    private String primaryFaultType;
    private List<HotspotRegion> anomalousRegions = new ArrayList<>();

    public Result() {
    }

    public Result(String status, double temperature, LocalDateTime time, String panelId) {
        this.status = status;
        this.temperature = temperature;
        this.time = time;
        this.panelId = panelId;
        this.filename = "";
        this.anomalyScore = 0.0;
        this.severity = "NORMAL";
        this.maxTemperature = temperature;
        this.faultCount = 0;
        this.primaryFaultType = "None";
    }

    public Result(String status, double temperature, LocalDateTime time, String panelId, double anomalyScore) {
        this.status = status;
        this.temperature = temperature;
        this.time = time;
        this.panelId = panelId;
        this.filename = "";
        this.anomalyScore = anomalyScore;
        this.severity = "NORMAL";
        this.maxTemperature = temperature;
        this.faultCount = 0;
        this.primaryFaultType = "None";
    }

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getPanelId() {
        return panelId;
    }

    public void setPanelId(String panelId) {
        this.panelId = panelId;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    // New thermal features getters/setters
    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public double getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(double maxTemperature) {
        this.maxTemperature = maxTemperature;
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

    public double getThermalGradient() {
        return thermalGradient;
    }

    public void setThermalGradient(double thermalGradient) {
        this.thermalGradient = thermalGradient;
    }

    public double getStdTemperature() {
        return stdTemperature;
    }

    public void setStdTemperature(double stdTemperature) {
        this.stdTemperature = stdTemperature;
    }

    // Filename
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename != null ? filename : "";
    }

    public int getFaultCount() {
        return faultCount;
    }

    public void setFaultCount(int faultCount) {
        this.faultCount = faultCount;
    }

    public String getPrimaryFaultType() {
        return primaryFaultType;
    }

    public void setPrimaryFaultType(String primaryFaultType) {
        this.primaryFaultType = primaryFaultType != null ? primaryFaultType : "None";
    }

    // Anomalous regions support
    public List<HotspotRegion> getAnomalousRegions() {
        return anomalousRegions;
    }

    public void setAnomalousRegions(List<HotspotRegion> anomalousRegions) {
        this.anomalousRegions = anomalousRegions != null ? anomalousRegions : new ArrayList<>();
    }

    public javafx.scene.image.Image getGeneratedHeatmap() { return generatedHeatmap; }
    public void setGeneratedHeatmap(javafx.scene.image.Image img) { this.generatedHeatmap = img; }

    public String getAnomalousRegionsSummary() {
        if (anomalousRegions.isEmpty())
            return "None";
        return anomalousRegions.stream()
                .map(r -> String.format("%s(%.1f°C)", r.getType(), r.getAvgTemperature()))
                .collect(java.util.stream.Collectors.joining(", "));
    }
}

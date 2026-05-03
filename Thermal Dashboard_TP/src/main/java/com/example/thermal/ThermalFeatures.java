package com.example.thermal;

public class ThermalFeatures {
    private double maxTemperature;
    private double avgTemperature;
    private double stdTemperature;
    private double thermalGradient;
    private double hotspotIntensity;

    public ThermalFeatures() {}

    public ThermalFeatures(double maxTemperature, double avgTemperature, double stdTemperature,
                          double thermalGradient, double hotspotIntensity) {
        this.maxTemperature = maxTemperature;
        this.avgTemperature = avgTemperature;
        this.stdTemperature = stdTemperature;
        this.thermalGradient = thermalGradient;
        this.hotspotIntensity = hotspotIntensity;
    }

    // Getters
    public double getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(double maxTemperature) { this.maxTemperature = maxTemperature; }

    public double getAvgTemperature() { return avgTemperature; }
    public void setAvgTemperature(double avgTemperature) { this.avgTemperature = avgTemperature; }

    public double getStdTemperature() { return stdTemperature; }
    public void setStdTemperature(double stdTemperature) { this.stdTemperature = stdTemperature; }

    public double getThermalGradient() { return thermalGradient; }
    public void setThermalGradient(double thermalGradient) { this.thermalGradient = thermalGradient; }

    public double getHotspotIntensity() { return hotspotIntensity; }
    public void setHotspotIntensity(double hotspotIntensity) { this.hotspotIntensity = hotspotIntensity; }

    @Override
    public String toString() {
        return String.format("ThermalFeatures{max=%.1f, avg=%.1f, std=%.1f, grad=%.1f, hotspot=%.1f}",
                maxTemperature, avgTemperature, stdTemperature, thermalGradient, hotspotIntensity);
    }
}


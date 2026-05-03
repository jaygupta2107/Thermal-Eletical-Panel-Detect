package com.example.thermal;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImageProcessor {

    /**
     * Extract thermal features matching notebook pipeline
     */
    public static ThermalFeatures extractFeatures(Mat src) {
        if (src.empty()) {
            return new ThermalFeatures();
        }

        int height = src.height();
        int width = src.width();
        Rect roiRect = new Rect((int) (width * 0.3), (int) (height * 0.3),
                (int) (width * 0.4), (int) (height * 0.4));

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // Normalize to 0-255
        Mat normalized = new Mat();
        Core.normalize(gray, normalized, 0, 255, Core.NORM_MINMAX);

        // Blurs
        Mat gaussianBlurred = new Mat();
        Imgproc.GaussianBlur(normalized, gaussianBlurred, new Size(5, 5), 0);
        Mat medianBlurred = new Mat();
        Imgproc.medianBlur(gaussianBlurred, medianBlurred, 5);

        Mat roi = new Mat(medianBlurred, roiRect);

        // Features (raw pixel values)
        double maxTempRaw = Core.minMaxLoc(roi).maxVal;
        Scalar meanScalar = Core.mean(roi);
        double avgTempRaw = meanScalar.val[0];

        // Pixel to temperature scaling: 20 + (pixel/255.0)*80 = 20-100°C
        double maxTemp = 20.0 + (maxTempRaw / 255.0) * 80.0;
        double avgTemp = 20.0 + (avgTempRaw / 255.0) * 80.0;

        // Std dev
        Mat diff = new Mat();
        Core.subtract(roi, new Scalar(avgTemp), diff);
        Mat sqDiff = new Mat();
        Core.multiply(diff, diff, sqDiff);
        Scalar varScalar = Core.mean(sqDiff);
        double stdTempRaw = Math.sqrt(varScalar.val[0]);
        double stdTemp = stdTempRaw * (80.0 / 255.0); // Scale std dev proportionally

        // Thermal gradient (Sobel magnitude mean)
        Mat sobelX = new Mat();
        Mat sobelY = new Mat();
        Imgproc.Sobel(roi, sobelX, CvType.CV_32F, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT);
        Imgproc.Sobel(roi, sobelY, CvType.CV_32F, 0, 1, 3, 1, 0, Core.BORDER_DEFAULT);
        Mat gradMag = new Mat();
        Core.magnitude(sobelX, sobelY, gradMag);
        double thermalGrad = Core.mean(gradMag).val[0];

        // Hotspot intensity (mean of hot pixels > avg + 2*std)
        double hotThresh = avgTemp + 2 * stdTemp;
        Mat hotMask = new Mat();
        Core.compare(roi, new Scalar(hotThresh), hotMask, Core.CMP_GT);
        Scalar hotMeanScalar = Core.mean(roi, hotMask);
        long hotCount = Core.countNonZero(hotMask);
        double hotspotIntRaw = hotCount > 0 ? hotMeanScalar.val[0] : 0.0;
        double hotspotInt = 20.0 + (hotspotIntRaw / 255.0) * 80.0;

        // Cleanup
        gray.release();
        normalized.release();
        gaussianBlurred.release();
        medianBlurred.release();
        roi.release();
        diff.release();
        sqDiff.release();
        sobelX.release();
        sobelY.release();
        gradMag.release();
        hotMask.release();

        return new ThermalFeatures(maxTemp, avgTemp, stdTemp, thermalGrad, hotspotInt);
    }

    /**
     * Get processed ROI image for visualization (blurred ROI with bounding rect and
     * hotspot overlay)
     */
    public static Mat getProcessedROIImage(Mat src) {
        if (src.empty()) {
            return new Mat();
        }

        int height = src.height();
        int width = src.width();
        Rect roiRect = new Rect((int) (width * 0.3), (int) (height * 0.3),
                (int) (width * 0.4), (int) (height * 0.4));

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Mat normalized = new Mat();
        Core.normalize(gray, normalized, 0, 255, Core.NORM_MINMAX);
        Mat gaussianBlurred = new Mat();
        Imgproc.GaussianBlur(normalized, gaussianBlurred, new Size(5, 5), 0);
        Mat roiProcessed = new Mat(gaussianBlurred, roiRect);

        // Draw ROI rect (will be full image size later)
        Mat vis = roiProcessed.clone();
        Scalar green = new Scalar(0, 255, 0);
        Imgproc.rectangle(vis, new Point(0, 0), new Point(roiRect.width, roiRect.height), green, 3);

        // Simple hotspot overlay (brighten hot pixels)
        Mat binaryHot = new Mat();
        Scalar threshHot = new Scalar(150); // empirical hot threshold
        Core.compare(vis, threshHot, binaryHot, Core.CMP_GT);
        vis.setTo(new Scalar(255, 255, 255), binaryHot); // white hotspot highlight

        // Cleanup intermediates
        gray.release();
        normalized.release();
        gaussianBlurred.release();
        binaryHot.release();

        return vis;
    }

    /**
     * Legacy: Process thermal image for anomaly detection (deprecated, use
     * extractFeatures)
     */
    public static Mat process(Mat src) {
        if (src.empty()) {
            return new Mat();
        }

        Mat gray = new Mat();
        Mat blurred = new Mat();
        Mat thresh = new Mat();

        // 1. Convert to grayscale
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // 2. Gaussian blur to reduce noise
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        // 3. Adaptive threshold for hot spot detection (thermal images: hot=white)
        Imgproc.adaptiveThreshold(blurred, thresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY, 15, 8);

        return thresh;
    }

    /**
     * Detect anomalous/hotspot regions specifically for electrical panels
     * Uses adaptive thresholding + morphology + contour analysis
     * Classifies based on position (breakers, busbar) and temperature
     */
    public static List<HotspotRegion> detectHotspotsElectrical(Mat src) {
        List<HotspotRegion> regions = new ArrayList<>();
        if (src.empty()) {
            return regions;
        }

        // Preprocess: grayscale, normalize
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Mat normalized = new Mat();
        Core.normalize(gray, normalized, 0, 255, Core.NORM_MINMAX);

        // Blurring
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(normalized, blurred, new Size(7, 7), 0);

        // Adaptive threshold for hotspots (hot = white)
        Mat thresh = new Mat();
        Imgproc.adaptiveThreshold(blurred, thresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 21, 10);

        // Morphology: close small gaps, then dilate
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Mat morphed = new Mat();
        Imgproc.morphologyEx(thresh, morphed, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(morphed, morphed, Imgproc.MORPH_DILATE, kernel);

        // Find contours
        java.util.List<org.opencv.core.MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(morphed, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int height = src.height();
        int width = src.width();
        double avgTemp = Core.mean(normalized).val[0];

        // Panel regions: breakers (bottom 30%), busbar (center 40% height, left/right)
        Rect busbarLeft = new Rect(0, (int) (height * 0.3), (int) (width * 0.4), (int) (height * 0.4));
        Rect busbarRight = new Rect((int) (width * 0.6), (int) (height * 0.3), (int) (width * 0.4),
                (int) (height * 0.4));

        for (org.opencv.core.MatOfPoint contour : contours) {
            org.opencv.core.Rect bbox = Imgproc.boundingRect(contour);
            double area = bbox.area();

            // Filter small noise/large invalid
            if (area < 0.002 * width * height || area > 0.1 * width * height)
                continue;

            // Mean temp in bbox
            Mat roiTemp = new Mat(normalized, bbox);
            double meanTempRaw = Core.mean(roiTemp).val[0];
            double meanTemp = 20.0 + (meanTempRaw / 255.0) * 80.0;
            roiTemp.release();

            double avgTempScaled = 20.0 + (avgTemp / 255.0) * 80.0;
            if (meanTemp < avgTempScaled + 10.0)
                continue; // Not hot enough (adjusted threshold)

            // Score: temp excess + relative size
            double tempScore = Math.max(0, (meanTemp - 50) / 40.0);
            double sizeScore = Math.min(1, area / (0.02 * width * height));
            double score = 0.7 * tempScore + 0.3 * sizeScore;

            String type = "HOTSPOT";
            // Classify by position
            if (bbox.y + bbox.height > height * 0.7) { // Bottom - breakers
                type = "BREAKER_OVERHEAT";
            } else if (overlaps(bbox, busbarLeft) || overlaps(bbox, busbarRight)) { // Busbars
                type = "LOOSE_CONNECTION";
            }

            regions.add(new HotspotRegion(bbox, meanTemp, meanTemp - avgTemp, score, type));
        }

        // Cleanup
        gray.release();
        normalized.release();
        blurred.release();
        thresh.release();
        morphed.release();
        kernel.release();
        hierarchy.release();

        return regions;
    }

    /**
     * Check if two Rects overlap
     */
    private static boolean overlaps(Rect r1, Rect r2) {
        return !(r1.br().y < r2.y || r1.y > r2.br().y ||
                r1.br().x < r2.x || r1.x > r2.br().x);
    }

    /**
     * Draw hotspot bounding boxes on image
     */
    public static Mat drawHotspots(Mat image, List<HotspotRegion> hotspots) {
        Mat vis = image.clone();
        for (HotspotRegion h : hotspots) {
            Rect bbox = h.getBoundingBox();
            Scalar color;
            switch (h.getType()) {
                case "BREAKER_OVERHEAT" -> color = new Scalar(0, 0, 255); // Red
                case "LOOSE_CONNECTION" -> color = new Scalar(0, 165, 255); // Orange
                case "HOTSPOT" -> color = new Scalar(0, 255, 0); // Green
                default -> color = new Scalar(255, 0, 0); // Blue
            }
            Imgproc.rectangle(vis, bbox.tl(), bbox.br(), color, 3);
            Imgproc.putText(vis, h.getType() + " (" + String.format("%.1f°C", h.getAvgTemperature()) + ")", bbox.tl(),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, color, 2);
        }
        return vis;
    }

    /**
     * Generates a full-colour heatmap from any thermal image using COLORMAP_JET.
     * Used for third-party images that have no pre-built heatwave in the dataset.
     *
     * Pipeline:
     *   1. Convert to grayscale
     *   2. Normalize to 0-255
     *   3. Apply COLORMAP_JET (blue=cool → red=hot)
     *   4. Overlay detected hotspot bounding boxes
     *   5. Return as BGR Mat ready for JavaFX display
     */
    public static Mat generateHeatmap(Mat src) {
        if (src.empty()) return new Mat();

        // 1. Grayscale
        Mat gray = new Mat();
        if (src.channels() == 1) {
            gray = src.clone();
        } else {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        }

        // 2. Normalize
        Mat normalized = new Mat();
        Core.normalize(gray, normalized, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);

        // 3. Smooth slightly before colormap
        Mat smoothed = new Mat();
        Imgproc.GaussianBlur(normalized, smoothed, new Size(5, 5), 0);

        // 4. Apply COLORMAP_JET
        Mat colormap = new Mat();
        Imgproc.applyColorMap(smoothed, colormap, Imgproc.COLORMAP_JET);

        // 5. Detect hotspots and draw bounding boxes on top
        List<HotspotRegion> hotspots = detectHotspotsElectrical(src);
        Mat result = drawHotspots(colormap, hotspots);

        // Cleanup
        gray.release();
        normalized.release();
        smoothed.release();
        colormap.release();

        return result;
    }

    /**
     * Convert an OpenCV Mat (BGR) to a JavaFX Image without writing to disk.
     * Uses PNG encoding into a byte buffer streamed directly to JavaFX Image.
     */
    public static javafx.scene.image.Image matToJavaFXImage(Mat mat) {
        if (mat.empty()) return null;
        // Encode to PNG bytes in memory
        org.opencv.core.MatOfByte mob = new org.opencv.core.MatOfByte();
        org.opencv.imgcodecs.Imgcodecs.imencode(".png", mat, mob);
        byte[] bytes = mob.toArray();
        mob.release();
        return new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes));
    }
}

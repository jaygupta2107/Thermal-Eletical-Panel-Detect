package com.example.thermal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class DashboardController {

    // ── FXML Fields ──────────────────────────────────────────────────────────────

    @FXML private TextField           panelIdField;
    @FXML private Button              uploadButton;
    @FXML private Button              processButton;
    @FXML private ImageView           originalImageView;
    @FXML private ImageView           resultImageView;
    @FXML private Label               severityLabel;
    @FXML private Label               maxTempLabel;
    @FXML private Label               avgTempLabel;
    @FXML private Label               hotspotIntensityLabel;
    @FXML private Label               thermalGradientLabel;
    @FXML private Label               stdTemperatureLabel;
    @FXML private Label               filenameLabel;
    @FXML private Label               recordCountLabel;
    @FXML private Label               resultPlaceholder;
    @FXML private ProgressIndicator   loadingSpinner;
    @FXML private Button              clearLogButton;
    @FXML private Button              exportButton;

    @FXML private TableView<Result>                   historyTable;
    @FXML private TableColumn<Result, String>    panelIdColumn;
    @FXML private TableColumn<Result, String>    filenameColumn;
    @FXML private TableColumn<Result, Double>    temperatureColumn;
    @FXML private TableColumn<Result, String>    statusColumn;
    @FXML private TableColumn<Result, String>    timeColumn;

    private TableColumn<Result, Integer> faultCountColumn;
    private TableColumn<Result, String>  primaryTypeColumn;
    private TableColumn<Result, String>  classesColumn;

    // ── State ────────────────────────────────────────────────────────────────────

    private final ObservableList<Result> results = FXCollections.observableArrayList();
    private File    selectedImageFile;
    private boolean isDatasetImage;   // true = from dataset, false = third-party

    // ── IMAGE SOURCE MODES ───────────────────────────────────────────────────────
    //
    //  MODE A — Dataset image  (Panel ID 1–85, file = orignal/panel_N.jpg)
    //    • Upload button opens to orignal/ folder
    //    • Uploaded filename must match "panel_<ID>.jpg"
    //    • On PROCESS → loads pre-built heatwaves/panel_<ID>.png  (instant)
    //
    //  MODE B — Third-party image  (any Panel ID, any thermal image file)
    //    • Upload button opens a general file chooser
    //    • Any image file accepted (no filename restriction)
    //    • On PROCESS → OpenCV generates COLORMAP_JET heatmap in real-time
    //    • Hotspot bounding boxes drawn on top of generated heatmap
    //    • Full anomaly detection still runs normally

    // ── Initialize ───────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        panelIdColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPanelId()));
        filenameColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getFilename()));
        temperatureColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getMaxTemperature()).asObject());
        statusColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getSeverity()));
        timeColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        faultCountColumn = new TableColumn<>("Fault Count");
        faultCountColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getFaultCount()).asObject());
        faultCountColumn.setPrefWidth(90);

        primaryTypeColumn = new TableColumn<>("Primary Fault");
        primaryTypeColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPrimaryFaultType()));
        primaryTypeColumn.setPrefWidth(110);

        classesColumn = new TableColumn<>("Classes");
        classesColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getAnomalousRegionsSummary()));
        classesColumn.setPrefWidth(120);

        @SuppressWarnings("unchecked")
        TableColumn<Result, ?>[] extra = new TableColumn[]{faultCountColumn, primaryTypeColumn, classesColumn};
        historyTable.getColumns().addAll(extra);

        panelIdColumn.setPrefWidth(110);
        filenameColumn.setPrefWidth(140);
        temperatureColumn.setPrefWidth(110);
        statusColumn.setPrefWidth(90);
        timeColumn.setPrefWidth(160);
        historyTable.setFixedCellSize(38.0);

        recordCountLabel.setText("0");
        resultPlaceholder.setVisible(true);
        results.addListener((javafx.collections.ListChangeListener<Result>)
                c -> recordCountLabel.setText(String.valueOf(results.size())));
        historyTable.setItems(results);

        // Upload disabled until Panel ID is entered
        uploadButton.setDisable(true);
        processButton.setDisable(true);

        panelIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            String trimmed = newVal.trim();
            boolean hasId = !trimmed.isEmpty() && trimmed.matches("\\d+");
            uploadButton.setDisable(!hasId);
            if (!hasId) {
                processButton.setDisable(true);
                resetPanels();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Returns true if panel_<id>.jpg exists in the bundled orignal/ resource folder. */
    private boolean isInDataset(int panelId) {
        return getClass().getResource(
                "/com/example/thermal/orignal/panel_" + panelId + ".jpg") != null;
    }

    private String expectedDatasetFilename(int panelId) {
        return "panel_" + panelId + ".jpg";
    }

    private String heatwaveResourcePath(int panelId) {
        return "/com/example/thermal/heatwaves/panel_" + panelId + ".png";
    }

    private void resetPanels() {
        originalImageView.setImage(null);
        resultImageView.setImage(null);
        resultPlaceholder.setText("Processed Heatmap Will Appear Here");
        resultPlaceholder.setVisible(true);
        filenameLabel.setText("File: No file selected");
        severityLabel.setText("STATUS: PENDING");
        severityLabel.getStyleClass().removeAll("normal", "warning", "critical");
        selectedImageFile = null;
        isDatasetImage = false;
    }

    // ── Upload ────────────────────────────────────────────────────────────────────

    @FXML
    private void handleUploadImage() {
        String panelIdText = panelIdField.getText().trim();
        int panelId;
        try {
            panelId = Integer.parseInt(panelIdText);
        } catch (NumberFormatException e) {
            showAlert("Invalid Panel ID", "Please enter a numeric Panel ID first.");
            return;
        }

        boolean datasetPanel = isInDataset(panelId);
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Thermal Images", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.tiff"));

        if (datasetPanel) {
            // MODE A — guide user to dataset folder
            fc.setTitle("Panel " + panelId + " — select  " + expectedDatasetFilename(panelId)
                    + "  or any thermal image");
            File resourceDir = resolveResourceDir("orignal");
            if (resourceDir != null && resourceDir.isDirectory())
                fc.setInitialDirectory(resourceDir);
        } else {
            // MODE B — third-party panel, open general chooser
            fc.setTitle("Panel " + panelId + " — Upload any thermal image (third-party)");
        }

        File chosen = fc.showOpenDialog(null);
        if (chosen == null) return;

        // Determine mode based on whether filename matches dataset pattern for this panel
        boolean matchesDataset = datasetPanel &&
                chosen.getName().equalsIgnoreCase(expectedDatasetFilename(panelId));

        if (datasetPanel && !matchesDataset) {
            // Dataset panel but wrong file — ask user which mode they want
            Alert modeAlert = new Alert(Alert.AlertType.CONFIRMATION);
            modeAlert.setTitle("Image Source");
            modeAlert.setHeaderText("File does not match dataset for Panel " + panelId);
            modeAlert.setContentText(
                    "Expected dataset file: " + expectedDatasetFilename(panelId) + "\n" +
                    "Selected: " + chosen.getName() + "\n\n" +
                    "Process as THIRD-PARTY image?\n" +
                    "(Heatmap will be generated by OpenCV in real-time)\n\n" +
                    "Click OK to continue as third-party, Cancel to re-select.");
            if (modeAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return;
            isDatasetImage = false;
        } else {
            isDatasetImage = matchesDataset;
        }

        selectedImageFile = chosen;
        originalImageView.setImage(new Image(chosen.toURI().toString()));

        String modeTag = isDatasetImage ? "[Dataset]" : "[Third-party — heatmap will be generated]";
        filenameLabel.setText("File: " + chosen.getName() + "  " + modeTag);

        resultImageView.setImage(null);
        resultPlaceholder.setText(isDatasetImage
                ? "Click PROCESS to load heatmap for Panel " + panelId
                : "Click PROCESS to generate heatmap from image");
        resultPlaceholder.setVisible(true);
        severityLabel.setText("STATUS: READY TO PROCESS");
        severityLabel.getStyleClass().removeAll("normal", "warning", "critical");

        processButton.setDisable(false);
    }

    // ── Process ───────────────────────────────────────────────────────────────────

    @FXML
    private void handleProcessImage() {
        String panelIdText = panelIdField.getText().trim();
        int panelId;
        try {
            panelId = Integer.parseInt(panelIdText);
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid Panel ID.");
            return;
        }
        if (selectedImageFile == null) {
            showAlert("No Image", "Please upload an image first.");
            return;
        }
        processImageAsync(panelId);
    }

    private void processImageAsync(int panelId) {
        loadingSpinner.setVisible(true);
        uploadButton.setDisable(true);
        processButton.setDisable(true);
        panelIdField.setDisable(true);
        resultPlaceholder.setVisible(true);
        resultImageView.setImage(null);

        final boolean useDataset = isDatasetImage;
        String panelIdStr = String.valueOf(panelId);

        Task<Result> task = new Task<Result>() {
            @Override
            protected Result call() {
                if (Main.isOpenCVLoaded()) {
                    try {
                        org.opencv.core.Mat src =
                                ImageUtils.matFromFile(selectedImageFile.getAbsolutePath());
                        if (!src.empty()) {
                            ValidationResult vr =
                                    Validator.validateImage(src, selectedImageFile.getName());
                            if (vr.isValid()) {
                                Result r = AnomalyDetector.detect(
                                        src, selectedImageFile.getName(), panelIdStr);

                                // For third-party images, generate heatmap now on bg thread
                                if (!useDataset) {
                                    org.opencv.core.Mat heatmat =
                                            ImageProcessor.generateHeatmap(src);
                                    javafx.scene.image.Image heatImg =
                                            ImageProcessor.matToJavaFXImage(heatmat);
                                    heatmat.release();
                                    // Pass generated image via userData
                                    r.setGeneratedHeatmap(heatImg);
                                }

                                src.release();
                                return r;
                            }
                            src.release();
                        }
                    } catch (Exception e) {
                        System.err.println("OpenCV error: " + e.getMessage());
                    }
                }
                return mockResult(panelIdStr);
            }
        };

        task.setOnSucceeded(e -> {
            Result result = task.getValue();

            if (useDataset) {
                // MODE A — load pre-built heatwave from resources
                loadDatasetHeatwave(panelId);
            } else {
                // MODE B — display the OpenCV-generated heatmap
                javafx.scene.image.Image generated = result.getGeneratedHeatmap();
                if (generated != null) {
                    resultImageView.setImage(generated);
                    resultPlaceholder.setVisible(false);
                    applySeverityBadge("READY");
                    // Tag the status to show it was generated
                    severityLabel.setText("STATUS: READY  [Generated]");
                } else {
                    resultPlaceholder.setText("Heatmap generation failed — OpenCV unavailable");
                    resultPlaceholder.setVisible(true);
                    applySeverityBadge("WARNING");
                }
            }

            updateMetrics(result);
            results.add(result);

            if (!"NORMAL".equals(result.getStatus())) {
                showAlert("Anomaly Alert",
                        "Anomaly detected — Panel " + panelId +
                        " | Max Temp: " + String.format("%.1f°C", result.getMaxTemperature()) +
                        " | Severity: " + result.getSeverity());
            }

            loadingSpinner.setVisible(false);
            uploadButton.setDisable(false);
            processButton.setDisable(false);
            panelIdField.setDisable(false);
        });

        task.setOnFailed(e -> {
            showAlert("Processing Failed", "Error: " + task.getException().getMessage());
            loadingSpinner.setVisible(false);
            uploadButton.setDisable(false);
            processButton.setDisable(true);
            panelIdField.setDisable(false);
            resultPlaceholder.setText("Processing Failed");
            resultPlaceholder.setVisible(true);
        });

        new Thread(task).start();
    }

    // ── Dataset Heatwave Loader ───────────────────────────────────────────────────

    /** MODE A: loads pre-built heatwaves/panel_<id>.png from bundled resources. */
    private void loadDatasetHeatwave(int panelId) {
        String path = heatwaveResourcePath(panelId);
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                Platform.runLater(() -> {
                    resultPlaceholder.setText("Heatwave not found: panel_" + panelId + ".png");
                    resultPlaceholder.setVisible(true);
                    applySeverityBadge("WARNING");
                });
                return;
            }
            Image img = new Image(is);
            Platform.runLater(() -> {
                resultImageView.setImage(img);
                resultPlaceholder.setVisible(false);
                applySeverityBadge("READY");
            });
        } catch (IOException ex) {
            Platform.runLater(() -> {
                resultPlaceholder.setText("Error loading heatwave");
                resultPlaceholder.setVisible(true);
                applySeverityBadge("ERROR");
            });
        }
    }

    // ── Severity Badge ────────────────────────────────────────────────────────────

    private void applySeverityBadge(String status) {
        severityLabel.getStyleClass().removeAll("normal", "warning", "critical");
        switch (status) {
            case "READY"   -> { severityLabel.setText("STATUS: READY");   severityLabel.getStyleClass().add("normal");   }
            case "WARNING" -> { severityLabel.setText("STATUS: WARNING"); severityLabel.getStyleClass().add("warning");  }
            case "ERROR"   -> { severityLabel.setText("STATUS: ERROR");   severityLabel.getStyleClass().add("critical"); }
            default        -> severityLabel.setText("STATUS: " + status);
        }
    }

    // ── Metrics ───────────────────────────────────────────────────────────────────

    private void updateMetrics(Result r) {
        if (maxTempLabel != null)
            maxTempLabel.setText("🌡 Max Temp: " + String.format("%.1f°C", r.getMaxTemperature()));
        if (avgTempLabel != null)
            avgTempLabel.setText("📊 Avg Temp: " + String.format("%.1f°C", r.getAvgTemperature()));
        if (hotspotIntensityLabel != null)
            hotspotIntensityLabel.setText(
                    String.format("🚨 Faults: %d (%s)", r.getFaultCount(), r.getPrimaryFaultType()));
        if (thermalGradientLabel != null)
            thermalGradientLabel.setText("🔥 Hotspots: " + r.getAnomalousRegionsSummary());
        if (stdTemperatureLabel != null)
            stdTemperatureLabel.setText("📈 Std Dev: " + String.format("%.1f", r.getStdTemperature()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Result mockResult(String panelId) {
        Result r = new Result("NORMAL", 45.0, java.time.LocalDateTime.now(), panelId, 0.1);
        r.setFilename(selectedImageFile != null ? selectedImageFile.getName() : "unknown.jpg");
        r.setSeverity("NORMAL");
        r.setFaultCount(0);
        r.setPrimaryFaultType("None");
        return r;
    }

    private File resolveResourceDir(String sub) {
        try {
            java.net.URL url = getClass().getResource("/com/example/thermal/" + sub + "/");
            if (url != null) return new File(url.toURI());
        } catch (Exception ignored) {}
        return null;
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    // ── History ───────────────────────────────────────────────────────────────────

    @FXML
    private void handleClearLog() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Log");
        confirm.setHeaderText("Clear all processing history?");
        confirm.setContentText("This cannot be undone.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK)
            results.clear();
    }

    @FXML
    private void handleExportCSV() {
        if (results.isEmpty()) { showAlert("Export", "No records to export."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Thermal Analysis History");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fc.showSaveDialog(null);
        if (file == null) return;
        try (FileWriter w = new FileWriter(file)) {
            w.append("Panel ID,Filename,Max Temperature (°C),Severity,Anomaly Score,Timestamp\n");
            for (Result r : results) {
                w.append(String.format("%s,%s,%.1f,%s,%.3f,%s\n",
                        r.getPanelId(), r.getFilename(), r.getMaxTemperature(),
                        r.getSeverity(), r.getAnomalyScore(),
                        r.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            }
            showAlert("Export Success",
                    String.format("Exported %d records to %s", results.size(), file.getName()));
        } catch (IOException ex) {
            showAlert("Export Error", "Failed to write CSV: " + ex.getMessage());
        }
    }
}

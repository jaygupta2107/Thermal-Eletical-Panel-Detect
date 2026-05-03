package com.example.thermal;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImageUtils {

    public static Image matToImage(Mat mat) {
        if (mat == null || mat.empty()) {
            return null;
        }

        Mat rgbMat = new Mat();
        if (mat.channels() > 1) {
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);
        } else {
            mat.copyTo(rgbMat);
        }

        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", rgbMat, mob);
        byte[] byteArray = mob.toArray();
        try {
            BufferedImage bufImage = ImageIO.read(new ByteArrayInputStream(byteArray));
            return SwingFXUtils.toFXImage(bufImage, null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Mat matFromFile(String filePath) {
        return Imgcodecs.imread(filePath);
    }
}


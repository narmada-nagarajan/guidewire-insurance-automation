package com.insurance.gw.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ScreenshotUtil {

    private ScreenshotUtil() {
    }

    public static byte[] asBytes(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    /** Saves a PNG under target/screenshots so a CI job can upload the folder as an artifact. */
    public static Path saveToDisk(WebDriver driver, String name) {
        try {
            Path dir = Paths.get("target", "screenshots");
            Files.createDirectories(dir);
            Path file = dir.resolve(name.replaceAll("[^a-zA-Z0-9-_]", "_") + ".png");
            Files.write(file, asBytes(driver));
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Could not save screenshot", e);
        }
    }
}

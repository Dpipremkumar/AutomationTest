package com.base;

import org.testng.ITestListener;
import org.testng.ITestResult;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        Object currentClass = result.getInstance();
        WebDriver driver = ((BaseTest) currentClass).driver;

        TakesScreenshot ts = (TakesScreenshot) driver;
        File screenshot = ts.getScreenshotAs(OutputType.FILE);

        try {
            Files.copy(screenshot.toPath(),
                    new File("screenshots/" + result.getName() + ".png").toPath());
            System.out.println("Screenshot captured for failed test: " + result.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

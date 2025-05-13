package com.fintech;

import com.base.BaseTest;
import com.base.TestListener;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Listeners(TestListener.class)
public class LoginFormTest {
    WebDriver driver;
    @BeforeTest
    public void lauchTheURL() {
        if (driver==null) {

            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver();
            driver.manage().window().maximize();
            driver.get("http://localhost:3000/");
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            Assert.assertTrue(driver.findElement(By.xpath("//div[contains(@class,'Home_page')]")).isDisplayed());
        }
    }
    @Test(priority = 1)
    public void testElementsAreRendered() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        WebElement usernameInput = driver.findElement(By.name("username"));
        WebElement passwordInput = driver.findElement(By.xpath("//input[@type='password']"));
        WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));

        Assert.assertTrue(usernameInput.isDisplayed());
        Assert.assertTrue(passwordInput.isDisplayed());
        Assert.assertTrue(loginButton.isDisplayed());
    }

    @Test(priority = 2)
    public void testButtonDisabledWhenFieldsEmpty() {
        WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));
        Assert.assertFalse(loginButton.isEnabled());
    }

    @Test(priority = 3)
    public void testLoginSuccessRedirect() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        WebElement userName =driver.findElement(By.name("username"));
        userName.clear();
        userName.sendKeys("testuser");
        WebElement psd =driver.findElement(By.xpath("//input[@type='password']"));
        psd.clear();
        psd.sendKeys("password123");
        WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));
        Assert.assertTrue(loginButton.isEnabled());
        loginButton.click();

        // Wait/Assert for redirection or success indicator
        String expectedUrl = "http://localhost:3000/dashboard"; // adjust accordingly
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                .until(d -> d.getCurrentUrl().equals(expectedUrl));

        Assert.assertEquals(driver.getCurrentUrl(), expectedUrl);
    }

}

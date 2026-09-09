package com.devops.accessportal.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public abstract class BaseSeleniumTest {

    protected WebDriver driver;
    protected WebDriverWait wait;

    protected static final String BASE_URL =
            "http://localhost:8081";

    protected static final String EMPLOYEE_EMAIL =
            "employee@accessportal.com";

    protected static final String EMPLOYEE_PASSWORD =
            "employee123";

    protected static final String REVIEWER_EMAIL =
            "reviewer@accessportal.com";

    protected static final String REVIEWER_PASSWORD =
            "reviewer123";

    @BeforeEach
    void setUp() {

        driver = new ChromeDriver();

        driver.manage().window().maximize();

        wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );
    }

    protected void openLoginPage() {

        driver.get(BASE_URL + "/login");

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.name("username")
                )
        );
    }

   protected void login(
        String email,
        String password) {

    System.out.println("==========================================");
    System.out.println("LOGIN START");
    System.out.println("Email: " + email);
    System.out.println("==========================================");

    openLoginPage();

    WebElement username =
            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.name("username")
                    )
            );

    username.clear();
    username.sendKeys(email);

    WebElement passwordField =
            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.name("password")
                    )
            );

    passwordField.clear();
    passwordField.sendKeys(password);

    driver.findElement(
            By.cssSelector("button[type='submit']")
    ).click();

    System.out.println("Login button clicked.");

    wait.until(
            ExpectedConditions.not(
                    ExpectedConditions.urlContains("/login")
            )
    );

    System.out.println(
            "Login completed. Current URL: "
                    + driver.getCurrentUrl()
    );
}

    protected void takeScreenshot(
            String testName) {

        try {

            File source =
                    ((TakesScreenshot) driver)
                            .getScreenshotAs(
                                    OutputType.FILE
                            );

            Path destination =
                    Path.of(
                            "target",
                            "screenshots",
                            testName + ".png"
                    );

            Files.createDirectories(
                    destination.getParent()
            );

            Files.copy(
                    source.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println(
                    "Failure screenshot saved: "
                            + destination
            );

        } catch (Exception e) {

            System.err.println(
                    "Could not capture screenshot: "
                            + e.getMessage()
            );
        }
    }

    protected void captureFailure(
            String testName) {

        takeScreenshot(testName);
    }

    @AfterEach
    void tearDown() {

        if (driver != null) {
            driver.quit();
        }
    }
}
/*
 protected void login(
            String email,
            String password) {

        openLoginPage();

        WebElement username =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.name("username")
                        )
                );

        username.clear();
        username.sendKeys(email);

        WebElement passwordField =
                driver.findElement(
                        By.name("password")
                );

        passwordField.clear();
        passwordField.sendKeys(password);

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.urlContains("/employee"),
                        ExpectedConditions.urlContains("/reviewer"),
                        ExpectedConditions.presenceOfElementLocated(
                                By.tagName("body")
                        )
                )
        );
    } */
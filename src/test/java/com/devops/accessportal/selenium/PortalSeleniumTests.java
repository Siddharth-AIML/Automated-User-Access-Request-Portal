package com.devops.accessportal.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PortalSeleniumTests extends BaseSeleniumTest {
@Test
void employeeLoginTest() {

    try {

        System.out.println("==========================================");
        System.out.println("EMPLOYEE TEST START");
        System.out.println("==========================================");

        login(
                EMPLOYEE_EMAIL,
                EMPLOYEE_PASSWORD
        );

        System.out.println("URL AFTER LOGIN: " + driver.getCurrentUrl());
        System.out.println("TITLE AFTER LOGIN: " + driver.getTitle());

        driver.get(
                BASE_URL + "/employee/dashboard"
        );

        System.out.println("URL AFTER DASHBOARD REQUEST: "
                + driver.getCurrentUrl());

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );

        System.out.println("==========================================");
        System.out.println("PAGE SOURCE START");
        System.out.println("==========================================");

        System.out.println(driver.getPageSource());

        System.out.println("==========================================");
        System.out.println("PAGE SOURCE END");
        System.out.println("==========================================");

        System.out.println(
                "Contains 'Employee Dashboard': "
                        + driver.getPageSource()
                        .contains("Employee Dashboard")
        );

        System.out.println(
                "Contains 'Dashboard': "
                        + driver.getPageSource()
                        .contains("Dashboard")
        );

        System.out.println(
                "FINAL URL: "
                        + driver.getCurrentUrl()
        );

        assertTrue(
                driver.getPageSource()
                        .contains("Employee Dashboard"),
                "Employee Dashboard was not displayed"
        );

        System.out.println("EMPLOYEE TEST PASSED");

    } catch (Exception e) {

        captureFailure("employeeLoginTest");

        throw e;
    }
}
    
    @Test
    void submitAccessRequestTest() {

        try {

            login(
                    EMPLOYEE_EMAIL,
                    EMPLOYEE_PASSWORD
            );

            driver.get(
                    BASE_URL + "/employee/requests/new"
            );

            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.name("resourceName")
                    )
            );

            driver.findElement(
                    By.name("resourceName")
            ).sendKeys(
                    "GitHub Repository"
            );

            driver.findElement(
                    By.name("accessType")
            ).sendKeys("READ");

            driver.findElement(
                    By.name("justification")
            ).sendKeys(
                    "Automated Selenium test request for Week 8."
            );

            driver.findElement(
                    By.cssSelector(
                            "button[type='submit']"
                    )
            ).click();

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.tagName("body")
                    )
            );

            assertTrue(
                    driver.getPageSource()
                            .contains("Request"),
                    "Request result page was not displayed"
            );

        } catch (Exception e) {

            captureFailure(
                    "submitAccessRequestTest"
            );

            throw e;
        }
    }

    @Test
    void viewRequestsTest() {

        try {

            login(
                    EMPLOYEE_EMAIL,
                    EMPLOYEE_PASSWORD
            );

            driver.get(
                    BASE_URL + "/employee/requests"
            );

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.tagName("body")
                    )
            );

            assertTrue(
                    driver.getPageSource()
                            .contains("My Access Requests"),
                    "My Access Requests page was not displayed"
            );

            assertTrue(
                    driver.getPageSource()
                            .contains("Status"),
                    "Request status was not displayed"
            );

        } catch (Exception e) {

            captureFailure(
                    "viewRequestsTest"
            );

            throw e;
        }
    }
/* */
    @Test
    void reviewerDashboardTest() {

        try {

            login(
                    REVIEWER_EMAIL,
                    REVIEWER_PASSWORD
            );

            driver.get(
                    BASE_URL + "/reviewer/dashboard"
            );

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.tagName("body")
                    )
            );

            assertTrue(
                    driver.getPageSource()
                            .contains("Reviewer Dashboard"),
                    "Reviewer Dashboard was not displayed"
            );

        } catch (Exception e) {

            captureFailure(
                    "reviewerDashboardTest"
            );

            throw e;
        }
    }
@Test
void reviewerApprovalInterfaceTest() {

    try {

        System.out.println("==========================================");
        System.out.println("REVIEWER TEST START");
        System.out.println("==========================================");

        login(
                REVIEWER_EMAIL,
                REVIEWER_PASSWORD
        );

        System.out.println("After login URL: " + driver.getCurrentUrl());

        System.out.println("After login title: " + driver.getTitle());

        System.out.println("==========================================");
        System.out.println("AFTER LOGIN PAGE SOURCE");
        System.out.println("==========================================");

        System.out.println(driver.getPageSource());

        System.out.println("==========================================");

        driver.get(
                BASE_URL + "/reviewer/dashboard"
        );

        System.out.println("Dashboard URL: " + driver.getCurrentUrl());

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );

        System.out.println("==========================================");
        System.out.println("REVIEWER DASHBOARD PAGE SOURCE");
        System.out.println("==========================================");

        String pageSource = driver.getPageSource();

        System.out.println(pageSource);

        System.out.println("==========================================");

        System.out.println(
                "Contains Approve: "
                        + pageSource.contains("Approve")
        );

        System.out.println(
                "Contains Review: "
                        + pageSource.contains("Review")
        );

        System.out.println(
                "Contains Reviewer: "
                        + pageSource.contains("Reviewer")
        );

        System.out.println(
                "Contains Dashboard: "
                        + pageSource.contains("Dashboard")
        );

        System.out.println(
                "FINAL URL: "
                        + driver.getCurrentUrl()
        );

        assertTrue(
                pageSource.contains("Approve")
                        || pageSource.contains("Review"),
                "Reviewer approval interface was not displayed"
        );

        System.out.println("==========================================");
        System.out.println("REVIEWER TEST PASSED");
        System.out.println("==========================================");

    } catch (Exception e) {

        captureFailure(
                "reviewerApprovalInterfaceTest"
        );

        throw e;
    }
}
}
/*
@Test
    void employeeLoginTest() {

        try {

            login(
                    EMPLOYEE_EMAIL,
                    EMPLOYEE_PASSWORD
            );

            driver.get(
                    BASE_URL + "/employee/dashboard"
            );

            assertTrue(
                    driver.getPageSource()
                            .contains("Employee Dashboard"),
                    "Employee Dashboard was not displayed"
            );

        } catch (Exception e) {

            captureFailure("employeeLoginTest");

            throw e;
        }
    }


    @Test
    void reviewerApprovalInterfaceTest() {

        try {

            login(
                    REVIEWER_EMAIL,
                    REVIEWER_PASSWORD
            );

            driver.get(
                    BASE_URL + "/reviewer/dashboard"
            );

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.tagName("body")
                    )
            );

            assertTrue(
                    driver.getPageSource()
                            .contains("Approve")
                            || driver.getPageSource()
                            .contains("Review"),
                    "Reviewer approval interface was not displayed"
            );

        } catch (Exception e) {

            captureFailure(
                    "reviewerApprovalInterfaceTest"
            );

            throw e;
        }
    } */
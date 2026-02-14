package com.team27.lucky3.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SidebarComponent {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(id = "sidebar-history")
    private WebElement rideHistoryLink;

    @FindBy(id = "sidebar-dashboard")
    private WebElement dashboardLink;

    public SidebarComponent(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateToRideHistory() {
        wait.until(ExpectedConditions.elementToBeClickable(rideHistoryLink));
        rideHistoryLink.click();
    }

    public void navigateToDashboard() {
        wait.until(ExpectedConditions.elementToBeClickable(dashboardLink));
        dashboardLink.click();
    }

    public boolean isRideHistoryLinkVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(rideHistoryLink));
            return rideHistoryLink.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}

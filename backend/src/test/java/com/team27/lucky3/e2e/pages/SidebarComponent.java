package com.team27.lucky3.e2e.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page object for the sidebar navigation component.
 * The sidebar starts collapsed; use {@link #openSidebar()} to expand it
 * before interacting with the links.
 */
public class SidebarComponent {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(css = "[data-testid='sidebar-toggle']")
    private WebElement sidebarToggleButton;

    @FindBy(id = "sidebar-history")
    private WebElement rideHistoryLink;

    @FindBy(id = "sidebar-dashboard")
    private WebElement dashboardLink;

    @FindBy(id = "sidebar-favorite")
    private WebElement favoritesLink;

    @FindBy(id = "sidebar-home")
    private WebElement homeLink;

    @FindBy(id = "sidebar-profile")
    private WebElement profileLink;

    @FindBy(id = "sidebar-support")
    private WebElement supportLink;

    public SidebarComponent(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void openSidebar() {
        wait.until(ExpectedConditions.elementToBeClickable(sidebarToggleButton));
        sidebarToggleButton.click();
    }

    public void navigateToRideHistory() {
        openSidebar();
        wait.until(ExpectedConditions.elementToBeClickable(rideHistoryLink));
        rideHistoryLink.click();
    }

    public void navigateToFavoriteRides() {
        openSidebar();
        wait.until(ExpectedConditions.elementToBeClickable(favoritesLink));
        favoritesLink.click();
    }

    public void navigateToDashboard() {
        openSidebar();
        wait.until(ExpectedConditions.elementToBeClickable(dashboardLink));
        dashboardLink.click();
    }

    public void navigateToHome() {
        openSidebar();
        wait.until(ExpectedConditions.elementToBeClickable(homeLink));
        homeLink.click();
    }

    public void navigateToProfile() {
        openSidebar();
        wait.until(ExpectedConditions.elementToBeClickable(profileLink));
        profileLink.click();
    }

    public void navigateToSupport() {
        openSidebar();
        wait.until(ExpectedConditions.elementToBeClickable(supportLink));
        supportLink.click();
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

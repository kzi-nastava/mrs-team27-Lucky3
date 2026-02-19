package com.team27.lucky3.e2e.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page object for the top navbar component.
 * Handles the sidebar toggle button and page title.
 */
public class NavbarComponent {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(css = "[data-testid='sidebar-toggle']")
    private WebElement sidebarToggleButton;

    @FindBy(css = "header h1")
    private WebElement pageTitle;

    public NavbarComponent(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    // Clicks the hamburger button to toggle the sidebar open/closed.
    public void toggleSidebar() {
        wait.until(ExpectedConditions.elementToBeClickable(sidebarToggleButton));
        sidebarToggleButton.click();
    }

    // Returns the page title text shown in the navbar.
    public String getPageTitle() {
        wait.until(ExpectedConditions.visibilityOf(pageTitle));
        return pageTitle.getText();
    }
}

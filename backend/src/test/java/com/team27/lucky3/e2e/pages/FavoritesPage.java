package com.team27.lucky3.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.List;

/**
 * Page Object class for the Favorites Page of the ride-sharing application.
 * Contains elements and actions for managing favorite routes.
 */
public class FavoritesPage {
    private WebDriver driver;
    private WebDriverWait wait;

    // Fixed: CSS does not support ':contains'. Used XPath for text matching.
    @FindBy(xpath = "//h2[contains(text(), 'Favorite Routes')]")
    private WebElement pageHeader;

    // Fixed: Simplified selector. 'min-w-full' is a unique enough class for the main table here.
    @FindBy(css = "table.min-w-full")
    private WebElement favoritesTable;

    // Fixed: Scoped to 'table' to avoid finding rows in other potential tables.
    @FindBy(css = "table tbody tr")
    private List<WebElement> favoriteRouteRows;

    // Kept: Standard Tailwind spinner class usually works well.
    @FindBy(css = ".animate-spin")
    private WebElement loadingSpinner;

    // Fixed: Tailwind classes with slashes (/) are hard to target in CSS without escaping.
    // XPath partial match is safer and cleaner here.
    @FindBy(xpath = "//div[contains(@class, 'bg-red-900') and contains(@class, 'border-red-800')]//p")
    private WebElement errorMessage;

    // Kept: XPath text search is correct for this.
    @FindBy(xpath = "//h3[contains(text(), 'No Favorite Routes')]")
    private WebElement emptyStateMessage;

    // Fixed: Locating by the "Total:" label is much more robust than relying on
    // the container's styling classes (bg-gray-900, etc.) which might change.
    @FindBy(xpath = "//span[contains(text(), 'Total:')]/following-sibling::span")
    private WebElement totalFavoritesCount;

    public FavoritesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(driver, this);
    }

    /**
     * Verifies that the favorites page is loaded.
     */
    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.visibilityOf(pageHeader));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Waits for the loading spinner to disappear.
     */
    public FavoritesPage waitForLoadingToComplete() {
        try {
            wait.until(ExpectedConditions.invisibilityOf(loadingSpinner));
        } catch (Exception e) {
        }
        return this;
    }

    /**
     * Gets the total count of favorite routes displayed.
     */
    public int getFavoriteRoutesCount() {
        waitForLoadingToComplete();
        int count = favoriteRouteRows.size();
        return count;
    }

    /**
     * Checks if the favorites table is displayed.
     */
    public boolean isFavoritesTableDisplayed() {
        try {
            return favoritesTable.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if empty state message is displayed.
     */
    public boolean isEmptyStateDisplayed() {
        try {
            return emptyStateMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets route name by index (0-based).
     */
    public String getRouteNameByIndex(int index) {
        if (index >= favoriteRouteRows.size()) {
            throw new IndexOutOfBoundsException("Route index out of bounds: " + index);
        }
        WebElement row = favoriteRouteRows.get(index);
        String routeName = row.findElement(By.cssSelector("td:nth-child(1) span.text-sm.font-medium")).getText();
        return routeName;
    }

    /**
     * Gets start location by index (0-based).
     */
    public String getStartLocationByIndex(int index) {
        if (index >= favoriteRouteRows.size()) {
            throw new IndexOutOfBoundsException("Route index out of bounds: " + index);
        }
        WebElement row = favoriteRouteRows.get(index);
        String startLocation = row.findElement(By.cssSelector("td:nth-child(2) span.text-sm")).getText();
        return startLocation;
    }

    /**
     * Gets destination location by index (0-based).
     */
    public String getDestinationLocationByIndex(int index) {
        if (index >= favoriteRouteRows.size()) {
            throw new IndexOutOfBoundsException("Route index out of bounds: " + index);
        }
        WebElement row = favoriteRouteRows.get(index);
        String destination = row.findElement(By.cssSelector("td:nth-child(3) span.text-sm")).getText();
        return destination;
    }

    /**
     * Clicks the 'Order' button for a specific favorite route by index.
     */
    public HomePage clickOrderButtonByIndex(int index) {
        if (index >= favoriteRouteRows.size()) {
            throw new IndexOutOfBoundsException("Route index out of bounds: " + index);
        }
        WebElement row = favoriteRouteRows.get(index);
        WebElement orderButton = row.findElement(By.xpath(".//button[contains(., 'Order')]"));

        wait.until(ExpectedConditions.elementToBeClickable(orderButton));
        orderButton.click();

        return new HomePage(driver);
    }

    /**
     * Clicks the 'Order' button for a favorite route by route name.
     */
    public HomePage clickOrderButtonByRouteName(String routeName) {
        WebElement targetRow = null;

        for (WebElement row : favoriteRouteRows) {
            String currentRouteName = row.findElement(By.cssSelector("td:nth-child(1) span.text-sm.font-medium")).getText();
            if (currentRouteName.equals(routeName)) {
                targetRow = row;
                break;
            }
        }

        if (targetRow == null) {
            throw new IllegalArgumentException("Route not found: " + routeName);
        }

        WebElement orderButton = targetRow.findElement(By.xpath(".//button[contains(., 'Order')]"));
        wait.until(ExpectedConditions.elementToBeClickable(orderButton));
        orderButton.click();

        return new HomePage(driver);
    }

    /**
     * Clicks the 'Remove' button for a favorite route by index.
     */
    public FavoritesPage clickRemoveButtonByIndex(int index) {
        if (index >= favoriteRouteRows.size()) {
            throw new IndexOutOfBoundsException("Route index out of bounds: " + index);
        }
        WebElement row = favoriteRouteRows.get(index);
        WebElement removeButton = row.findElement(By.xpath(".//button[contains(., 'Remove')]"));

        wait.until(ExpectedConditions.elementToBeClickable(removeButton));
        removeButton.click();

        return this;
    }

    /**
     * Verifies if a specific route exists by name.
     */
    public boolean isRouteExists(String routeName) {
        for (WebElement row : favoriteRouteRows) {
            String currentRouteName = row.findElement(By.cssSelector("td:nth-child(1) span.text-sm.font-medium")).getText();
            if (currentRouteName.equals(routeName)) {
                return true;
            }
        }
        return false;
    }
}
package com.team27.lucky3.e2e.pages;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * Page Object class for the Home Page of the ride-sharing application.
 * Contains elements and actions for the main map view and ride ordering functionality.
 */
public class HomePage{
    private WebDriver driver;
    private WebDriverWait wait;
    private static final String URL = "http://localhost:4200/login";

    @FindBy(id = "map")
    private WebElement mapContainer;

    // Fixed: CSS :contains is not valid. Used XPath to find span with text "Lucky Ride" following the logo div.
    @FindBy(xpath = "//div[contains(@class, 'bg-yellow-500/20')]/following-sibling::span[contains(text(), 'Lucky Ride')]")
    private WebElement logoText;

    // Fixed: Simplified to find the button containing the specific text.
    @FindBy(xpath = "//button[contains(., 'Order Ride')]")
    private WebElement orderRideButton;

    // Fixed: Escaped the forward slash for the Tailwind class in CSS selector.
    // This panel appears after clicking "Order Ride".
    @FindBy(css = "div.bg-gray-900\\/95")
    private WebElement orderingFormPanel;

    // Fixed: Replaced invalid :contains CSS with XPath.
    @FindBy(xpath = "//h3[contains(@class, 'text-white') and contains(text(), 'Ordering Ride')]")
    private WebElement orderingFormHeader;

    @FindBy(css = "input[placeholder='Pickup address']")
    private WebElement pickupAddressInput;

    @FindBy(css = "input[placeholder='Destination address']")
    private WebElement destinationAddressInput;

    // XPath is robust here for finding input based on sibling text content
    @FindBy(xpath = "//label[.//div[contains(text(), 'Standard')]]//input[@type='radio']")
    private WebElement standardVehicleRadio;

    @FindBy(xpath = "//label[.//div[contains(text(), 'Luxury')]]//input[@type='radio']")
    private WebElement luxuryVehicleRadio;

    @FindBy(xpath = "//label[.//div[contains(text(), 'Van')]]//input[@type='radio']")
    private WebElement vanVehicleRadio;

    @FindBy(xpath = "//label[.//span[contains(text(), 'Pet Transport')]]//input[@type='checkbox']")
    private WebElement petTransportCheckbox;

    @FindBy(xpath = "//label[.//span[contains(text(), 'Baby Transport')]]//input[@type='checkbox']")
    private WebElement babyTransportCheckbox;

    @FindBy(xpath = "//button[contains(text(), 'Now')]")
    private WebElement scheduleNowButton;

    // Updated to match "In..." or similar variations if the text contains ellipsis character
    @FindBy(xpath = "//button[contains(text(), 'In')]")
    private WebElement scheduleInButton;

    // Matches button with "Order" or "Recalculate" text
    @FindBy(xpath = "//button[contains(text(), 'Order') or contains(text(), 'Recalculate')]")
    private WebElement submitOrderButton;

    @FindBy(xpath = "//button[contains(text(), 'Clear')]")
    private WebElement clearButton;

    // Fixed: Escaped slash in CSS selector for red background error message
    @FindBy(css = "div.bg-red-500\\/10")
    private WebElement errorMessage;

    // Fixed: Escaped slash in CSS selector for result panel
    @FindBy(css = "div.bg-gray-800\\/50")
    private WebElement estimationResultPanel;

    @FindBy(xpath = "//div[contains(text(), 'Distance')]/following-sibling::div")
    private WebElement distanceValue;

    @FindBy(xpath = "//div[contains(text(), 'Duration')]/following-sibling::div")
    private WebElement durationValue;

    @FindBy(xpath = "//div[contains(text(), 'Est. Price')]/following-sibling::div")
    private WebElement estimatedPriceValue;

    // Finds the spinner SVG inside the yellow button
    @FindBy(css = "button.bg-yellow-500 svg.animate-spin")
    private WebElement loadingSpinner;

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(driver, this);
    }

    /**
     * Verifies that the home page is loaded by checking the map container.
     */
    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.visibilityOf(mapContainer));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clicks the 'Order Ride' button to open the ordering form.
     */
    public HomePage clickOrderRideButton() {
        wait.until(ExpectedConditions.elementToBeClickable(orderRideButton));
        orderRideButton.click();
        waitForOrderingFormToOpen();
        return this;
    }

    /**
     * Waits for the ordering form panel to be visible.
     */
    private void waitForOrderingFormToOpen() {
        wait.until(ExpectedConditions.visibilityOf(orderingFormPanel));
    }

    /**
     * Checks if the ordering form is displayed.
     */
    public boolean isOrderingFormDisplayed() {
        try {
            return orderingFormPanel.isDisplayed() && orderingFormHeader.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the value from the pickup address input field.
     */
    public String getPickupAddress() {
        wait.until(ExpectedConditions.visibilityOf(pickupAddressInput));
        String address = pickupAddressInput.getAttribute("value");
        return address;
    }

    /**
     * Gets the value from the destination address input field.
     */
    public String getDestinationAddress() {
        wait.until(ExpectedConditions.visibilityOf(destinationAddressInput));
        String address = destinationAddressInput.getAttribute("value");
        return address;
    }

    /**
     * Checks if a specific vehicle type is selected by default.
     */
    public boolean isVehicleTypeSelected(String vehicleType) {
        WebElement radioButton = getVehicleRadioButton(vehicleType);
        boolean isSelected = radioButton.isSelected();
        return isSelected;
    }

    /**
     * Helper method to get the appropriate vehicle radio button.
     */
    private WebElement getVehicleRadioButton(String vehicleType) {
        switch (vehicleType.toLowerCase()) {
            case "standard":
                return standardVehicleRadio;
            case "luxury":
                return luxuryVehicleRadio;
            case "van":
                return vanVehicleRadio;
            default:
                throw new IllegalArgumentException("Invalid vehicle type: " + vehicleType);
        }
    }

    /**
     * Checks if pet transport checkbox is selected.
     */
    public boolean isPetTransportSelected() {
        boolean isSelected = petTransportCheckbox.isSelected();
        return isSelected;
    }

    /**
     * Checks if baby transport checkbox is selected.
     */
    public boolean isBabyTransportSelected() {
        boolean isSelected = babyTransportCheckbox.isSelected();
        return isSelected;
    }

    /**
     * Checks if 'Now' scheduling option is selected by default.
     */
    public boolean isScheduleNowSelected() {
        String classValue = scheduleNowButton.getAttribute("class");
        boolean isSelected = classValue.contains("bg-yellow-500");
        return isSelected;
    }

    /**
     * Enters pickup address in the input field.
     */
    public HomePage enterPickupAddress(String address) {
        wait.until(ExpectedConditions.visibilityOf(pickupAddressInput));
        pickupAddressInput.clear();
        pickupAddressInput.sendKeys(address);
        return this;
    }

    /**
     * Enters destination address in the input field.
     */
    public HomePage enterDestinationAddress(String address) {
        wait.until(ExpectedConditions.visibilityOf(destinationAddressInput));
        destinationAddressInput.clear();
        destinationAddressInput.sendKeys(address);
        return this;
    }

    /**
     * Navigates to Favorites page by URL.
     */
    public FavoritesPage navigateToFavorites() {
        String currentUrl = driver.getCurrentUrl();
        String baseUrl = currentUrl.contains("/home") ? currentUrl.replace("/home", "/favorites") : currentUrl + "/favorites";
        driver.get(baseUrl);
        return new FavoritesPage(driver);
    }
}
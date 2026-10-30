package au.gov.qld.online.selenium;

import org.openqa.selenium.WebDriver;

public class WebDriverHolder {

    private final WebDriver webDriver;
    private final DriverTypes driverType;
    private int numberUsed = 0;
    private final String downloadDirectory;
    private final boolean isGrid;

    public WebDriverHolder(WebDriver webDriver, DriverTypes driverType, String downloadDirectory, boolean isGrid) {
        this.webDriver = webDriver;
        this.driverType = driverType;
        this.downloadDirectory = downloadDirectory;
        this.isGrid = isGrid;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public DriverTypes getDriverType() {
        return driverType;
    }

    public String getBrowserName() {
        return driverType.name();
    }

    public int incrementUsed() {
        numberUsed++;
        return numberUsed;
    }

    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    public boolean isGrid() {
        return isGrid;
    }
}

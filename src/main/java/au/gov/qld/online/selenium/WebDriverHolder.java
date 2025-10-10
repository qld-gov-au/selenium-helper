package au.gov.qld.online.selenium;

import org.openqa.selenium.WebDriver;

public class WebDriverHolder {

    private final WebDriver webDriver;
    private final DriverTypes driverType;
    private int numberUsed = 0;
    private final String downloadDirectory;

    public WebDriverHolder(WebDriver webDriver, DriverTypes driverType, String downloadDirectory) {
        this.webDriver = webDriver;
        this.driverType = driverType;
        this.downloadDirectory = downloadDirectory;
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
}

package au.gov.qld.online.selenium;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.service.DriverService;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariDriverService;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.Platform.MAC;
import static org.openqa.selenium.Platform.WINDOWS;

public final class SeleniumHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_BROWSER_USAGE = 10;

    private static DriverService chromeService;

    //Keep list of released browsers to reuse until max usage is hit
    private static Map<String, WebDriverHolder> webDriverListReleased = new ConcurrentHashMap<>();
    //Keep internal tabs on open browsers so when we die unexpectedly we don't leave orphaned browsers running on outside of jvm connection
    private static List<WebDriver> webDriverListAll = new LinkedList<>();
    private static List<DriverService> driverServiceAll = new LinkedList<>();
    private static File screenprintFolder = new File("target/screenprints/" + new SimpleDateFormat("dd-M-yyyy", Locale.getDefault()).format(new Date()) + "/");
    private static File screenprintCurrentFolder = new File("target/screenprints/current");
    private static boolean doScreenPrints = false;
    private static boolean headlessEnabled = true;

    private static Proxy proxy;
    /**
     * This cleans up anything that used this helper class
     */
    private static final Thread CLOSE_THREAD = new Thread() {
        @Override
        public void run() {
            for (WebDriver driver : webDriverListAll) {
                if (driver != null) {
                    try {
                        driver.close();
                        driver.quit();
                    } catch (RuntimeException e) {
                        LOGGER.error("exception on close", e);
                    }
                }
            }
            for (DriverService service : driverServiceAll) {
                if (service != null) {
                    try {
                        service.stop();
                    } catch (RuntimeException e) {
                        LOGGER.error("exception on close", e);
                    }
                }
            }
        }
    };

    static {
        if (StringUtils.isNotBlank(System.getProperty("headless.disabled"))) {
            LOGGER.debug("headless disabled");
            headlessEnabled = false;
        }
        if (StringUtils.isNotBlank(System.getProperty("doScreenPrints"))) {
            LOGGER.debug("screenprints enabled");
            doScreenPrints = true;
        }
        if (StringUtils.isNotBlank(System.getProperty("http_proxy"))) {
            LOGGER.debug("proxy enabled");
            proxy = new Proxy();
            proxy.setHttpProxy(System.getProperty("http_proxy", ""));
            proxy.setSslProxy(System.getProperty("https_proxy", ""));
        }
        Runtime.getRuntime().addShutdownHook(CLOSE_THREAD);
        try {
            FileUtils.forceMkdir(screenprintFolder);
            if (screenprintCurrentFolder.exists()) {
                FileUtils.forceDelete(screenprintCurrentFolder);
            }
            Files.createSymbolicLink(Paths.get(screenprintCurrentFolder.getAbsolutePath()), Paths.get(screenprintFolder.getAbsolutePath()));
        } catch (IOException e) {
            LOGGER.error("could not create screenprint folder");
        }
    }

    private SeleniumHelper() {
        //utility class
    }

    public static File getDestinationFolder() {
        return screenprintFolder;
    }

    public static synchronized WebDriverHolder getWebDriver(DriverTypes driverType) {
        //reuse any active session that was released
        for (String key: webDriverListReleased.keySet()) {
            WebDriverHolder driver = webDriverListReleased.get(key);
            if (driverType.equals(driver.getDriverType())) {
                webDriverListReleased.remove(key);
                return driver;
            }
        }

        WebDriver webDriver = null;

        try {
            final Platform platform = Platform.getCurrent();
            switch (driverType) {
                case CHROME:
                    setupChromeService();
                    DesiredCapabilities capabilities = new DesiredCapabilities();
                    final ChromeOptions chromeOptions = new ChromeOptions();
                    if (headlessEnabled) {
                        chromeOptions.setHeadless(true);
                    }
                    chromeOptions.merge(capabilities);
                    webDriver = new RemoteWebDriver(chromeService.getUrl(), chromeOptions);
                    break;
                case FIREFOX:
                    WebDriverManager.firefoxdriver().setup();
                    final FirefoxOptions firefoxOptions = new FirefoxOptions();
                    if (headlessEnabled) {
                        firefoxOptions.setHeadless(true);
                    }
                    if (proxy != null) {
                        firefoxOptions.setProxy(proxy);
                    }
                    firefoxOptions.addPreference("devtools.jsonview.enabled", false);
                    GeckoDriverService geckoDriverService = new GeckoDriverService.Builder().usingAnyFreePort().build();
                    geckoDriverService.start();
                    driverServiceAll.add(geckoDriverService);
                    webDriver = new FirefoxDriver(geckoDriverService, firefoxOptions);
                    //can't wrap remotewebdriver for firefox but chrome etc can
                    //webDriver = new RemoteWebDriver(firefoxService.getUrl(), firefoxOptions);
                    break;
                case EDGE:
                    if (platform.is(WINDOWS)) {
                        WebDriverManager.edgedriver().setup();
                        final EdgeOptions edgeOptions = new EdgeOptions();
                        if (proxy != null) {
                            edgeOptions.setProxy(proxy);
                        }
                        EdgeDriverService edgeDriverService = new EdgeDriverService.Builder().usingAnyFreePort().build();
                        driverServiceAll.add(edgeDriverService);
                        webDriver = new EdgeDriver(edgeDriverService, edgeOptions);
                    } else {
                        throw new IllegalStateException("Have to be on windows to run Edge");
                    }
                    break;
                case SAFARI:
                    if (platform.is(MAC)) {
                        WebDriverManager.edgedriver().setup();
                        DesiredCapabilities safariCapabilities = new DesiredCapabilities();
                        final SafariOptions safariOptions = new SafariOptions();
                        safariOptions.merge(safariCapabilities);
                        if (proxy != null) {
                            safariOptions.setProxy(proxy);
                        }
                        SafariDriverService safariDriverService = new SafariDriverService.Builder().usingAnyFreePort().build();
                        driverServiceAll.add(safariDriverService);
                        webDriver = new SafariDriver(safariDriverService, safariOptions);
                    } else {
                        throw new IllegalStateException("Have to be on Mac to run Safari");
                    }
                    break;
                case HtmlUnitDriverWithJS:
                    webDriver = createHtmlUnitDriver(true);
                    break;
                case HtmlUnitDriver:
                    webDriver = createHtmlUnitDriver(false);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown DriverTypes");
            }

            webDriver.manage().deleteAllCookies();
            webDriver.manage().timeouts().pageLoadTimeout(360, TimeUnit.SECONDS);
            webDriver.manage().window().maximize();
            Dimension maximizeDim = webDriver.manage().window().getSize();
            LOGGER.info("Size of screen. Height: " + maximizeDim.getHeight() + ", Width:" + maximizeDim.getWidth());
        } catch (RuntimeException ex) {
            LOGGER.error("Exception in initiating a browser session");
            LOGGER.error("Error Message: ", ex);
            throw ex;
        } catch (Exception e) {
            LOGGER.error("Exception in initiating a browser session");
            LOGGER.error("Error Message: ", e);
            throw new IllegalStateException(e);
        }
        WebDriverHolder holder = new WebDriverHolder(webDriver, driverType);
        webDriverListAll.add(webDriver);
        return holder;
    }

    private static HtmlUnitDriver createHtmlUnitDriver(final boolean enableJavascript) {
        HtmlUnitDriver driver;
        if (enableJavascript) {
            driver = new HtmlUnitDriver(BrowserVersion.BEST_SUPPORTED);
            driver.setJavascriptEnabled(true);
        } else {
            driver = new HtmlUnitDriver(false) {
                @Override
                protected WebClient modifyWebClient(WebClient client) {
                    client.getOptions().setCssEnabled(false);
                    return client;
                }
            };
        }
        if (proxy != null) {
            driver.setProxySettings(proxy);
        }
        return driver;
    }

    /**
     * If this is called, we will put the browser into the unused pool or destroy it if used more than 10 times
     * We have found that the WAF's on uat.identity.qld.gov.au does not like it if it sees the same browser fingerprint
     * too many times, this may be true for other systems also.
     * @param webDriverHolder
     */
    public static void close(WebDriverHolder webDriverHolder) {
        if (webDriverHolder == null) {
            return;
        }

        WebDriver driver = webDriverHolder.getWebDriver();
        driver.manage().deleteAllCookies();

        driver.navigate().to("about:blank");

        if (webDriverHolder.incrementUsed() > MAX_BROWSER_USAGE) {
            webDriverHolder.getWebDriver().close();
            webDriverHolder.getWebDriver().quit();
        } else {
            webDriverListReleased.put(String.valueOf(webDriverHolder.hashCode()), webDriverHolder);
        }
    }

    /**
     * This will do a deep clean of the browser
     * Currently only does chrome
     * @param webDriverHolder
     */
    public static void forceClearAll(WebDriverHolder webDriverHolder) {
        WebDriver driver = webDriverHolder.getWebDriver();
        if (webDriverHolder.getBrowserName().equalsIgnoreCase(DriverTypes.CHROME.name())) {
            driver.navigate().to("chrome://settings/clearBrowserData");
            WebDriverWait waiter = new WebDriverWait(driver, 30, 500);
            String ccs = "* /deep/ #clearBrowsingDataConfirm";
            waiter.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(ccs)));
            driver.findElement(By.cssSelector("* /deep/ #clearBrowsingDataConfirm")).click();
            waiter.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(ccs)));
        }
    }

    public static boolean isDoScreenPrints() {
        return doScreenPrints;
    }

    public static void setDoScreenPrints(boolean doScreenPrints) {
        SeleniumHelper.doScreenPrints = doScreenPrints;
    }

    private static int shotsTaken = 0;
    public static boolean performScreenPrint(WebDriverHolder webDriverHolder, String testName) {
        if (webDriverHolder.getWebDriver() instanceof TakesScreenshot) {
            if (SeleniumHelper.isDoScreenPrints()) {
                File scrFile = ((TakesScreenshot) webDriverHolder.getWebDriver()).getScreenshotAs(OutputType.FILE);
                try {
                    FileUtils.copyFile(scrFile, new File(SeleniumHelper.getDestinationFolder().getPath(), getScreenPrintFilename(webDriverHolder, testName)));
                } catch (IOException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static String getScreenPrintFilename(WebDriverHolder webdriver, String testName) {
        return (shotsTaken++)
            + "-" + testName
            + "-" + webdriver.getBrowserName() + ".png";
    }

    private static synchronized void setupChromeService() throws IOException {
        if (chromeService != null) {
            return;
        }

        WebDriverManager.chromedriver().setup();
        chromeService = new ChromeDriverService.Builder()
            .usingDriverExecutable(new File(WebDriverManager.chromedriver().getBinaryPath()))
            .usingAnyFreePort()
            .build();
        driverServiceAll.add(chromeService);
        chromeService.start();
    }

    public static int openDrivers() {
        return webDriverListAll.size();
    }

    public static int webDriverReleasedSize() {
        return webDriverListReleased.size();
    }
}

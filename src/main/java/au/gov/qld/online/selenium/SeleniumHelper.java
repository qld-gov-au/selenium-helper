package au.gov.qld.online.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

import static org.openqa.selenium.Platform.MAC;
import static org.openqa.selenium.Platform.WINDOWS;

@SuppressWarnings("PMD.AvoidCatchingGenericException") //generic catch's needed for simpler cleanup
public final class SeleniumHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static int maxBrowserUsage = 10;

    private static DriverService chromeService;

    //Keep list of released browsers to reuse until max usage is hit
    private static Map<String, WebDriverHolder> webDriverListReleased = new ConcurrentHashMap<>();
    //Keep internal tabs on open browsers so when we die unexpectedly we don't leave orphaned browsers running on outside of jvm connection
    private static List<WebDriver> webDriverListAll = new LinkedList<>();
    private static List<DriverService> driverServiceAll = new LinkedList<>();
    private static File screenprintFolder = new File("target/screenprints/" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "/");
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
            closeAllBrowsers();
            for (DriverService service : driverServiceAll) {
                if (service != null) {
                    try (service) {
                        service.stop();
                    } catch (Exception e) {
                        LOGGER.error("exception on close", e);
                    }
                }
            }
        }
    };

    public static void closeAllBrowsers() {
        Iterator<WebDriver> iterator = webDriverListAll.iterator();
        while (iterator.hasNext()) {
            WebDriver driver = iterator.next();
            iterator.remove();
            if (driver != null) {
                try {
                    driver.close();
                    driver.quit();
                } catch (Exception e) {
                    LOGGER.error("exception on close", e);
                }
            }
        }
        webDriverListReleased.clear();
    }

    static {
        if (StringUtils.isNotBlank(System.getProperty("headless.disabled"))) {
            LOGGER.debug("headless disabled");
            headlessEnabled = false;
        }
        if (StringUtils.isNotBlank(getEnvIgnoreCase("headless_disabled"))) {
            LOGGER.debug("headless disabled");
            headlessEnabled = false;
        }
        if (StringUtils.isNotBlank(System.getProperty("doScreenPrints"))) {
            LOGGER.debug("screenprints enabled");
            doScreenPrints = true;
        }
        if (StringUtils.isNotBlank(getEnvIgnoreCase("doScreenPrints"))) {
            LOGGER.debug("screenprints enabled");
            doScreenPrints = true;
        }

        proxyConfig();

        Runtime.getRuntime().addShutdownHook(CLOSE_THREAD);
        try {
            FileUtils.forceMkdir(screenprintFolder);
            if (screenprintCurrentFolder.exists()) {
                FileUtils.forceDelete(screenprintCurrentFolder);
            }
            screenprintFolder.setWritable(true);
            Files.createSymbolicLink(Paths.get(screenprintCurrentFolder.getAbsolutePath()), Paths.get(screenprintFolder.getAbsolutePath()));
        } catch (IOException e) {
            LOGGER.error("could not create screenprint folder");
        }
    }

    private SeleniumHelper() {
        //utility class
    }

    private static String getEnvIgnoreCase(String key) {
        for (Map.Entry<String, String> k : System.getenv().entrySet()) {
            if (k.getKey().equalsIgnoreCase(key)) {
                return k.getValue();
            }
        }
        return null;
    }

    public static void setProxy(Proxy proxyReplace) {
        proxy = proxyReplace;
    }

    public static Proxy getProxy() {
        return proxy;
    }

    public static void proxyConfig() {
        proxy = new Proxy();
        proxy.setProxyType(Proxy.ProxyType.DIRECT);
        String httpsProxy = getEnvIgnoreCase("https_proxy");
        String httpProxy = getEnvIgnoreCase("http_proxy");
        String nonProxyHosts = getEnvIgnoreCase("http_nonProxyHosts");

        if (httpProxy != null && !httpProxy.isEmpty()) {
            URI uriHttp = URI.create(httpProxy);
            if (uriHttp.getHost() != null) {
                System.setProperty("http.proxyHost", uriHttp.getHost());
            }
            if (uriHttp.getPort() != -1) {
                System.setProperty("http.proxyPort", String.valueOf(uriHttp.getPort()));
            }
        }

        if (httpsProxy != null && !httpsProxy.isEmpty()) {
            URI uriHttps = URI.create(httpsProxy);

            if (uriHttps.getHost() != null) {
                System.setProperty("https.proxyHost", uriHttps.getHost());
            }
            if (uriHttps.getPort() != -1) {
                System.setProperty("https.proxyPort", String.valueOf(uriHttps.getPort()));
            }
        }

        if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
            //Ensure java proxy is pipe delimited.
            System.setProperty("http.nonProxyHosts", nonProxyHosts.replaceAll(",", "|"));
        }

        String systemHttpsProxyHost = System.getProperty("https.proxyHost");
        String systemHttpProxyHost = System.getProperty("http.proxyHost");
        String systemNonProxyHosts = System.getProperty("http.nonProxyHosts");
        if ((systemHttpsProxyHost != null && !systemHttpsProxyHost.isEmpty())
                || (systemHttpProxyHost != null && !systemHttpProxyHost.isEmpty())
        ) {
            try {
                proxy = new Proxy();
                proxy.setProxyType(Proxy.ProxyType.MANUAL);
                if (systemHttpProxyHost != null && !systemHttpProxyHost.isEmpty()) {
                    proxy.setHttpProxy(systemHttpProxyHost + ":" + System.getProperty("http.proxyPort", "80"));
                }
                if (systemHttpsProxyHost != null && !systemHttpsProxyHost.isEmpty()) {
                    proxy.setSslProxy(systemHttpsProxyHost + ":" + System.getProperty("https.proxyPort", "443"));
                }

                if (systemNonProxyHosts != null && !systemNonProxyHosts.isEmpty()) {
                    //Java default http.nonProxyHosts: A list of hosts that should be reached directly, bypassing the proxy.
                    // This is a list of patterns separated by |. The patterns may start or end with a * for wildcards.
                    // Any host that matches one of these patterns is reached through a direct connection instead of through a proxy.
                    // But Selenium wants it in comma delimited form.
                    proxy.setNoProxy(systemNonProxyHosts.replaceAll("\\|", ","));
                }
            } catch (Exception e) {
                LOGGER.error("could not create proxy", e);
            }
        }
    }

    public static File getDestinationFolder() {
        return screenprintFolder;
    }

    public static synchronized WebDriverHolder getWebDriver(DriverTypes driverType) {
        return getWebDriver(driverType, null);
    }

    @SuppressWarnings("PMD.ExhaustiveSwitchHasDefault")
    public static synchronized WebDriverHolder getWebDriver(DriverTypes driverType, String downloadDirectory) {
        //reuse any active session that was released if the download directory has not been set
        for (String key : webDriverListReleased.keySet()) {
            WebDriverHolder driver = webDriverListReleased.get(key);
            if (driverType == driver.getDriverType() && StringUtils.equals(downloadDirectory, driver.getDownloadDirectory())) {
                webDriverListReleased.remove(key);
                return driver;
            }
        }

        WebDriver webDriver;
        WebDriverManager wdm;
        try {
            final Platform platform = Platform.getCurrent();
            final String browserDownloadOption = "browser.download.dir";
            switch (driverType) {
                case CHROME:
                    setupChromeService();
                    DesiredCapabilities capabilities = new DesiredCapabilities();
                    final ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--host-resolver-rules=MAP www.google-analytics.com 127.0.0.1, www.googletagmanager.com 127.0.0.1");
                    chromeOptions.addArguments("--disable-gpu");
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-extensions");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                    chromeOptions.addArguments("--crash-dumps-dir=/tmp");
                    if (headlessEnabled) {
                        chromeOptions.addArguments("--headless=new");
                    }
                    if (downloadDirectory != null) {
                        Map<String, Object> chromePrefs = new HashMap<>();
                        chromePrefs.put("download.default_directory", downloadDirectory);
                        chromePrefs.put("plugins.always_open_pdf_externally", true);
                        chromePrefs.put("download.prompt_for_download", false);
                        chromePrefs.put("profile.default_content_settings.popups", 0);
                        chromeOptions.setExperimentalOption("prefs", chromePrefs);
                    }
                    chromeOptions.addArguments("--remote-allow-origins=*");
                    chromeOptions.merge(capabilities);
                    if (proxy != null) {
                        chromeOptions.setProxy(proxy);
                    }
                    webDriver = new RemoteWebDriver(chromeService.getUrl(), chromeOptions);
                    break;
                case FIREFOX:
                    wdm = WebDriverManager.firefoxdriver();
                    final FirefoxOptions firefoxOptions = new FirefoxOptions();
                    wdm.config().setTimeout(30);
                    if (proxy != null) {
                        firefoxOptions.setProxy(proxy);
                        wdm.config().setProxy(proxy.getHttpProxy());
                    }
                    wdm.setup();
                    if (headlessEnabled) {
                        firefoxOptions.addArguments("-headless");
                    }
                    if (downloadDirectory != null) {
                        firefoxOptions.addPreference("browser.download.folderList", 2);
                        firefoxOptions.addPreference(browserDownloadOption, downloadDirectory);
                        firefoxOptions.addPreference("browser.download.useDownloadDir", true);
                    }
                    firefoxOptions.addPreference("devtools.jsonview.enabled", false);
                    GeckoDriverService geckoDriverService = new GeckoDriverService.Builder().usingAnyFreePort().build();
                    geckoDriverService.start();
                    driverServiceAll.add(geckoDriverService);
                    webDriver = new FirefoxDriver(geckoDriverService, firefoxOptions);
                    break;
                case EDGE:
                    if (platform.is(WINDOWS)) {
                        wdm = WebDriverManager.edgedriver();
                        wdm.config().setTimeout(30);
                        final EdgeOptions edgeOptions = new EdgeOptions();
                        if (proxy != null) {
                            edgeOptions.setProxy(proxy);
                            wdm.config().setProxy(proxy.getHttpProxy());
                        }
                        wdm.setup();
                        if (downloadDirectory != null) {
                            Map<String, Object> edgePrefs = new HashMap<>();
                            edgePrefs.put("download.default_directory", downloadDirectory);
                            edgePrefs.put("plugins.always_open_pdf_externally", true);
                            edgePrefs.put("download.prompt_for_download", false);
                            edgePrefs.put("profile.default_content_settings.popups", 0);
                            edgeOptions.setExperimentalOption("prefs", edgePrefs);
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
                        wdm = WebDriverManager.edgedriver();
                        wdm.config().setTimeout(30);
                        DesiredCapabilities safariCapabilities = new DesiredCapabilities();
                        final SafariOptions safariOptions = new SafariOptions();
                        safariOptions.merge(safariCapabilities);
                        if (proxy != null && proxy.getProxyType() != Proxy.ProxyType.DIRECT) {
                            safariOptions.setProxy(proxy);
                            wdm.config().setProxy(proxy.getHttpProxy());
                        }
                        wdm.setup();
                        if (downloadDirectory != null) {
                            LOGGER.error("browser.download.dir - it is no longer supported in W3C/Safari combo, downloads will only go to user.home Downloads folder.");
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

            if (!(DriverTypes.SAFARI == driverType)) {
                webDriver.manage().deleteAllCookies();
            }
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(360));
            webDriver.manage().window().maximize();
            Dimension maximizeDim = webDriver.manage().window().getSize();
            LOGGER.info("Size of screen. Height: {}, Width: {}", maximizeDim.getHeight(), maximizeDim.getWidth());
        } catch (RuntimeException ex) {
            LOGGER.error("Exception in initiating a browser session");
            LOGGER.error("Error Message: ", ex);
            throw ex;
        } catch (Exception e) {
            LOGGER.error("Exception in initiating a browser session");
            LOGGER.error("Error Message: ", e);
            throw new IllegalStateException(e);
        }
        WebDriverHolder holder = new WebDriverHolder(webDriver, driverType, downloadDirectory);
        webDriverListAll.add(webDriver);
        return holder;
    }

    private static HtmlUnitDriver createHtmlUnitDriver(final boolean enableJavascript) {
        HtmlUnitDriver driver;
        //Mimic google chrome latest so we have esm6 support
        driver = new HtmlUnitDriver(BrowserVersion.CHROME) {
            @Override
            protected WebClient modifyWebClient(WebClient client) {
                client.getOptions().setThrowExceptionOnScriptError(false);
                client.getOptions().setJavaScriptEnabled(enableJavascript);
                return client;
            }
        };

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
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public static void close(WebDriverHolder webDriverHolder, boolean clearCookies) {
        if (webDriverHolder == null) {
            return;
        }

        WebDriver driver = webDriverHolder.getWebDriver();
        if (clearCookies) {
            if (DriverTypes.SAFARI == webDriverHolder.getDriverType()) {
                LOGGER.error("SAFARI does not allow cookie delete :'( normally throws org.openqa.selenium.NoSuchSessionException");
            } else {
                driver.manage().deleteAllCookies();
            }
        }

        driver.navigate().to("about:blank");

        if (webDriverHolder.incrementUsed() > maxBrowserUsage) {
            try {
                webDriverHolder.getWebDriver().close();
                webDriverHolder.getWebDriver().quit();
            } catch (Exception e) {
                LOGGER.error("Error Message: ", e);
            }
        } else {
            webDriverListReleased.put(String.valueOf(webDriverHolder.hashCode()), webDriverHolder);
        }
    }

    /**
     * If this is called, we will put the browser into the unused pool or destroy it if used more than 10 times
     * We have found that the WAF's on uat.identity.qld.gov.au does not like it if it sees the same browser fingerprint
     * too many times, this may be true for other systems also.
     * @param webDriverHolder
     */
    public static void close(WebDriverHolder webDriverHolder) {
        close(webDriverHolder, true);
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

            WebDriverWait waiter = new WebDriverWait(driver, Duration.ofSeconds(30), Duration.ofSeconds(500));
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
        shotsTaken++;
        return shotsTaken
            + "-" + testName
            + "-" + webdriver.getBrowserName() + ".png";
    }

    private static synchronized void setupChromeService() throws IOException {
        if (chromeService != null) {
            return;
        }

        WebDriverManager wdm = WebDriverManager.chromedriver();
        wdm.config().setTimeout(30);
        if (proxy != null) {
            wdm.config().setProxy(proxy.getHttpProxy());
        }
        wdm.setup();
        chromeService = new ChromeDriverService.Builder()
            .usingDriverExecutable(new File(wdm.getDownloadedDriverPath()))
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

    public static int getMaxBrowserUsage() {
        return maxBrowserUsage;
    }

    public static void setMaxBrowserUsage(int maxBrowserUsage) {
        SeleniumHelper.maxBrowserUsage = maxBrowserUsage;
    }

}

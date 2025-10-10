package au.gov.qld.online.selenium;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SeleniumHelperTest {

    private WebDriverHolder holder;

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
        SeleniumHelper.close(holder, true);
    }

    @Test
    //@Disabled("ubuntu has issues on profile popup which you can't get past atm")
    public void shouldStartFirefoxBrowser() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        holder = SeleniumHelper.getWebDriver(DriverTypes.FIREFOX);
        holder.getWebDriver().navigate().to("https://google.com/");
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.close(holder);
    }

    @Test
    public void shouldStartFirefoxBrowserMultiTest() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        holder = SeleniumHelper.getWebDriver(DriverTypes.FIREFOX);
        WebDriverHolder holder2 = SeleniumHelper.getWebDriver(DriverTypes.FIREFOX);
        holder.getWebDriver().navigate().to("https://google.com/");
        SeleniumHelper.performScreenPrint(holder2, testName);
        holder2.getWebDriver().navigate().to("https://site-down.services.qld.gov.au");
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.performScreenPrint(holder2, testName);
        SeleniumHelper.close(holder);
        SeleniumHelper.close(holder2);

        WebDriverHolder holder3 = SeleniumHelper.getWebDriver(DriverTypes.FIREFOX);
        holder3.getWebDriver().navigate().to("https://www.whirlpool.net.au");
        SeleniumHelper.performScreenPrint(holder3, testName);
        SeleniumHelper.close(holder);
    }

    @Test
    public void shouldStartFirefoxChromeBrowserMultiMixTest() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        holder = SeleniumHelper.getWebDriver(DriverTypes.FIREFOX);
        WebDriverHolder holder2 = SeleniumHelper.getWebDriver(DriverTypes.CHROME);
        holder.getWebDriver().navigate().to("https://google.com/");
        SeleniumHelper.performScreenPrint(holder2, testName);
        holder2.getWebDriver().navigate().to("https://site-down.services.qld.gov.au");
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.performScreenPrint(holder2, testName);
        SeleniumHelper.close(holder);
        SeleniumHelper.close(holder2);

        WebDriverHolder holder3 = SeleniumHelper.getWebDriver(DriverTypes.FIREFOX);
        holder3.getWebDriver().navigate().to("https://www.whirlpool.net.au");
        SeleniumHelper.performScreenPrint(holder3, testName);
        SeleniumHelper.close(holder);
    }

    @Test
    public void shouldStartEdgeBrowser() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        try {
            holder = SeleniumHelper.getWebDriver(DriverTypes.EDGE);
            holder.getWebDriver().navigate().to("https://google.com/");
            SeleniumHelper.performScreenPrint(holder, testName);
        } catch (RuntimeException e) {
            // Expected when not on windows
            assertThat(e.getMessage()).isEqualTo("Have to be on windows to run Edge");
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void shouldStartSafariBrowser() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        try {
            holder = SeleniumHelper.getWebDriver(DriverTypes.SAFARI);
            holder.getWebDriver().navigate().to("https://google.com/");
            SeleniumHelper.performScreenPrint(holder, testName);
        } catch (RuntimeException e) {
            // Expected when not on mac
            assertThat(e.getMessage()).isEqualTo("Have to be on Mac to run Safari");
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void shouldStartChromeBrowser() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        holder = SeleniumHelper.getWebDriver(DriverTypes.CHROME);
        holder.getWebDriver().navigate().to("https://google.com/");
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.close(holder);
    }

    @Test
    public void shouldStartChromeBrowserWithTwoInaRow() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        holder = SeleniumHelper.getWebDriver(DriverTypes.CHROME);
        holder.getWebDriver().navigate().to("https://google.com/");
        WebDriverHolder holder2 = SeleniumHelper.getWebDriver(DriverTypes.CHROME);
        holder2.getWebDriver().navigate().to("https://site-down.services.qld.gov.au");
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.performScreenPrint(holder2, testName);
        SeleniumHelper.close(holder);
        SeleniumHelper.close(holder2);

        WebDriverHolder holder3 = SeleniumHelper.getWebDriver(DriverTypes.CHROME);
        SeleniumHelper.performScreenPrint(holder3, testName);
        holder3.getWebDriver().navigate().to("https://www.whirlpool.net.au");
        SeleniumHelper.performScreenPrint(holder3, testName);
        SeleniumHelper.close(holder3);
    }

    @Test
    public void shouldStartHtmlUnitBrowser() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        holder = SeleniumHelper.getWebDriver(DriverTypes.HtmlUnitDriver);
        holder.getWebDriver().navigate().to("https://google.com/");
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.close(holder);
    }

    @Test
    public void shouldStartHtmlUnitWithJsBrowser() {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        holder = SeleniumHelper.getWebDriver(DriverTypes.HtmlUnitDriverWithJS);
        holder.getWebDriver().navigate().to("https://www.bing.com/");
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.close(holder);
    }

    @Test
    public void shouldSetDownloadDirectoryForFirefoxBrowser() throws IOException {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        Path tempDownloadDirectory = Files.createTempDirectory("tempDownloads");
        tempDownloadDirectory.toFile().deleteOnExit();
        String downloadFilepath = tempDownloadDirectory.toFile().getAbsolutePath() + "/";
        holder = SeleniumHelper.getWebDriver(DriverTypes.FIREFOX, downloadFilepath);
        holder.getWebDriver().navigate().to("about:preferences");
        assertThat(holder.getWebDriver().findElement(new By.ByXPath("//*[@id='downloadFolder']")).getAttribute("style")).containsIgnoringCase(downloadFilepath);
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.close(holder);
    }

    @Test
    public void shouldSetDownloadDirectoryForChromeBrowser() throws IOException, InterruptedException {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        // chrome://settings/downloads uses "#shadow-root" which prevents using xpath to view the download directory, so try download instead
        Path tempDownloadDirectory = Files.createTempDirectory("tempDownloads");
        tempDownloadDirectory.toFile().deleteOnExit();
        String downloadFilepath = tempDownloadDirectory.toFile().getAbsolutePath() + "/";
        String filename = "queensland-first-home-owners-grant-by-statistical-area_2018-19-to-31dec18.csv";
        holder = SeleniumHelper.getWebDriver(DriverTypes.CHROME, downloadFilepath);
        // Download a test file from staging.data.qld.gov.au: "testfile.xlsx"
        holder.getWebDriver().navigate().to("https://www.data.qld.gov.au/dataset/c4aa9fdf-0f43-4617-8afa-efaf43d5e533/resource/fd3b2d83-b198-487a-8845-0e47792ce2e5/download/queensland-first-home-owners-grant-by-statistical-area_2018-19-to-31dec18.csv");
        FluentWait<WebDriver> wait = new FluentWait<>(holder.getWebDriver()).withTimeout(Duration.ofSeconds(10L)).pollingEvery(Duration.ofMillis(100));
        File downloadedFile = new File(downloadFilepath + filename);
        wait.until(x -> downloadedFile.exists());
        try {
            Assertions.assertThat(downloadedFile).exists();
        } catch (AssertionError e) {
            Set<String> files = Stream.of(Objects.requireNonNull(tempDownloadDirectory.toFile().listFiles()))
                    .filter(file -> !file.isDirectory())
                    .map(File::getName)
                    .collect(Collectors.toSet());
            System.out.println("Could not find file, found: " + String.join(", ", files));
            throw e;
        }
        downloadedFile.deleteOnExit();
        SeleniumHelper.performScreenPrint(holder, testName);
        SeleniumHelper.close(holder);
    }

    // Need Selenium 4 before custom download directory for Edge is implemented
    /* @Test
    public void shouldSetDownloadDirectoryForEdgeBrowser() throws IOException {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        Path tempDownloadDirectory = Files.createTempDirectory("tempDownloads");
        tempDownloadDirectory.toFile().deleteOnExit();
        String downloadFilepath = tempDownloadDirectory.toFile().getAbsolutePath() + "/";
        try {
            holder = SeleniumHelper.getWebDriver(DriverTypes.EDGE, downloadFilepath);
            holder.getWebDriver().navigate().to("edge://settings/downloads");
            assertThat(holder.getWebDriver().findElement(new By.ByXPath("//p[contains(text(), '" + downloadFilepath + "')]")).isDisplayed()).isTrue();
            SeleniumHelper.performScreenPrint(holder, testName);
        } catch (RuntimeException e) {
            // Expected when not on windows
            assertThat(e.getMessage()).isEqualTo("Have to be on windows to run Edge");
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        SeleniumHelper.close(holder);
    } */

    @Test
    public void shouldSetDownloadDirectoryForSafariBrowser() throws IOException, InterruptedException {
        String testName = new Object(){}.getClass().getEnclosingMethod().getName();
        SeleniumHelper.setDoScreenPrints(true);
        Path tempDownloadDirectory = Files.createTempDirectory("tempDownloads");
        tempDownloadDirectory.toFile().deleteOnExit();
        String downloadFilepath = tempDownloadDirectory.toFile().getAbsolutePath() + "/";
        String filename = "testfile.xlsx";
        // Not sure how safari has settings set up, so try download instead
        try {
            holder = SeleniumHelper.getWebDriver(DriverTypes.SAFARI, downloadFilepath);
            // Download a test file from staging.data.qld.gov.au: "testfile.xlsx"
            holder.getWebDriver().navigate().to("https://staging.data.qld.gov.au/dataset/534a6213-bc2e-4b79-a8da-95438adf47f9/resource/c5098e4c-994d-48d7-a12b-c69c876897cc/download/testfile.xlsx");
            TimeUnit.SECONDS.sleep(3);
            File downloadedFile = new File(downloadFilepath + filename);
            Assertions.assertThat(downloadedFile).exists();
            downloadedFile.deleteOnExit();
            SeleniumHelper.performScreenPrint(holder, testName);
            SeleniumHelper.close(holder);
        } catch (RuntimeException e) {
            // Expected when not on mac
            assertThat(e.getMessage()).isEqualTo("Have to be on Mac to run Safari");
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
    }

    // Custom download directories haven't beeen implemented for HtmlUnitBrowser and HtmlUnitWithJsBrowser
}

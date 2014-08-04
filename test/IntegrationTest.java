import com.google.common.base.Function;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import play.libs.F.Callback;
import play.test.TestBrowser;
import play.test.TestServer;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

/**
 * Tests the game engine by starting a server and connecting two browsers.
 */
public class IntegrationTest {

    /**
     * {@link play.test.Helpers#running(play.test.TestServer, Class, play.libs.F.Callback)} alternative that requires no servers.
     */
    protected static synchronized void _running(Class<? extends WebDriver> webDriver, final Callback<TestBrowser> block) {
        TestBrowser browser = null;
        try {
            browser = testBrowser(play.api.test.WebDriverFactory.apply(webDriver));
            block.invoke(browser);
        }
        catch (Error | RuntimeException e) { throw e; }
        catch (Throwable t) { throw new RuntimeException(t); }
        finally { if (browser != null) browser.quit(); }
    }

    private static void waitForStatus(TestBrowser browser, final String text) {
        browser.waitUntil(new Function<WebDriver, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable WebDriver input) {
                if (input == null) return false;
                WebElement status;
                try { status = input.findElement(By.cssSelector("#status span")); }
                catch (NoSuchElementException nsee) { return false; }
                return status.getText().equals(text);
            }
        });
    }

    @Test
    public void test() {
        int port = 3333;
        final TestServer server = testServer(port);
        final String serverURL = String.format("http://localhost:%d/", port);
        final Class<? extends WebDriver> driver = FIREFOX;

        // Start the web server and the first browser.
        running(server, driver, new Callback<TestBrowser>() {
            public void invoke(final TestBrowser fstBrowser) {

                // Let the first browser to join the game.
                fstBrowser.goTo(serverURL);
                assertThat(fstBrowser.pageSource())
                        .contains("There are 0 game(s) and 0 pending player(s) online.");

                // Wait until the WebSocket connection gets established and
                // game engine adds us to the pending players queue.
                waitForStatus(fstBrowser, "Connected. Waiting for opponent...");

                // Start a second browser.
                _running(driver, new Callback<TestBrowser>() {
                    public void invoke(TestBrowser sndBrowser) {
                        sndBrowser.goTo(serverURL);

                        // Make sure that we observe the pending first player.
                        assertThat(sndBrowser.pageSource())
                                .contains("There are 0 game(s) and 1 pending player(s) online.");

                        // Wait until getting paired with the first user.
                        waitForStatus(sndBrowser, "Paired. Waiting for move...");

                        // Make sure that the first browser is also informed of pairing.
                        waitForStatus(fstBrowser, "Paired. Waiting for move...");

                        // Close the second browser.
                        sndBrowser.quit();

                        // Make sure that the first browser observes the lost connection.
                        waitForStatus(fstBrowser, "Connection lost!");
                    }
                });
            }
        });
    }

}

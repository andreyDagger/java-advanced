package info.kgeorgiy.ja.matveev.bank;

import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * App that runs {@link BankTest} tests
 *
 * @author AndreyMatveev
 * @since 21
 */
public class BankTests {
    public static void main(String[] args) {
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(BankTest.class)).build();
        launcher.execute(launcher.discover(request));

        if (listener.getSummary().getFailures().isEmpty()) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
}

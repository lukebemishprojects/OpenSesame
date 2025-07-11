package dev.lukebemish.opensesame.test.framework;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

public class Framework {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectModule(Framework.class.getModule().getName())
                )
                .filters(
                        includeEngines("junit-jupiter", "opensesame-layered-framework")
                )
                .configurationParameter("junit.platform.reporting.open.xml.enabled", "true")
                .configurationParameter("junit.platform.reporting.output.dir", "results")
                .build();

        try (LauncherSession session = LauncherFactory.openSession()) {
            TestPlan testPlan = session.getLauncher().discover(request);
            
            var summaryGeneratingService = new SummaryGeneratingListener();
            AtomicBoolean hasFailures = new AtomicBoolean(false);
            
            var errorPrintingService = new TestExecutionListener() {
                @Override
                public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                    if (testExecutionResult.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
                        hasFailures.set(true);
                        testExecutionResult.getThrowable().ifPresentOrElse(
                                throwable -> {
                                    System.err.println(testIdentifier.getDisplayName()+": failed with " + throwable);
                                    throwable.printStackTrace(System.err);
                                },
                                () -> System.err.println(testIdentifier.getDisplayName()+": failed")
                        );
                    }
                    TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
                }
            };

            session.getLauncher().registerTestExecutionListeners(summaryGeneratingService, errorPrintingService);

            session.getLauncher().execute(testPlan);
            summaryGeneratingService.getSummary().printTo(new PrintWriter(System.out));
            
            if (hasFailures.get()) {
                throw new RuntimeException("Some tests failed. See the output above for details.");
            }
        }
    }
}

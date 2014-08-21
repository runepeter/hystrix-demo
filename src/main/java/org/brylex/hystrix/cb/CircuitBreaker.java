package org.brylex.hystrix.cb;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import java.util.concurrent.Executors;

public class CircuitBreaker {

    private static class CircuitBreakerCommand extends HystrixCommand<String> {

        protected CircuitBreakerCommand() {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("CircuitBreakerGroup"))
                            .andCommandKey(HystrixCommandKey.Factory.asKey("GetAccounts"))
                            .andCommandPropertiesDefaults(
                                    HystrixCommandProperties.Setter()
                                            .withCircuitBreakerEnabled(true)
                                            .withCircuitBreakerErrorThresholdPercentage(15)
                                            .withCircuitBreakerSleepWindowInMilliseconds(500)
                            )
            );
        }

        @Override
        protected String run() throws Exception {

            double random = Math.random();
            try {
                Thread.sleep((int) (random * 200) + 50);
            } catch (InterruptedException e) {
            }

            if (random > 0.7777) {
                throw new RuntimeException("random failure loading order over network");
            }

            return "NORMAL";
        }

        @Override
        protected String getFallback() {
            if (isCircuitBreakerOpen()) {
                return "SHORT-CIRCUIT";
            } else {
                return "FALLBACK";
            }
        }
    }

    public static void main(String[] args) throws Exception {

        startJetty();

        startHealthLogger();

        while (true) {

            CircuitBreakerCommand command = new CircuitBreakerCommand();

            String result = command.execute();

            System.err.printf("RESULT: [%s].\n", result);
        }
    }

    private static void startHealthLogger() {
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.metrics.rollingPercentile.numBuckets", 60);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {

                HystrixCommandMetrics metrics = null;

                while (true) {

                    if (metrics == null) {
                        metrics = HystrixCommandMetrics.getInstance(HystrixCommandKey.Factory.asKey("GetAccounts"));
                    } else {
                        System.out.printf("Error count: %s\nTotal: %s\nPercentage: %s.\n", metrics.getHealthCounts().getErrorCount(), metrics.getHealthCounts().getTotalRequests(), metrics.getHealthCounts().getErrorPercentage());
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });
    }

    private static void startJetty() throws Exception {
        final Server server = new Server(9091);

        ServletHandler hystrixStreamHandler = new ServletHandler();
        hystrixStreamHandler.addServletWithMapping(HystrixMetricsStreamServlet.class, "/hystrix/hystrix.stream");

        server.setHandler(hystrixStreamHandler);

        server.start();
    }
}

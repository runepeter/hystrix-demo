package org.brylex.hystrix.timeout;

import org.brylex.hystrix.AccountService;
import org.brylex.hystrix.rest.AccountServiceProxy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Timeout {

    private static class TimeoutAccountService implements AccountService {

        private final AccountServiceProxy proxy = new AccountServiceProxy();

        private final ExecutorService executors = Executors.newSingleThreadExecutor();

        @Override
        public List<String> get(final String... accountIds) {

            final Future<List<String>> future = executors.submit(new Callable<List<String>>() {
                @Override
                public List<String> call() throws Exception {
                    return proxy.get(accountIds);
                }
            });

            try {
                return future.get(5l, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static void main(String[] args) throws Exception {

        final AccountService service = new TimeoutAccountService();

        System.err.printf("RESULT: [%s].\n", service.get("runepeter"));
    }
}

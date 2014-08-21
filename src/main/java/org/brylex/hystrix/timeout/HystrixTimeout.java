package org.brylex.hystrix.timeout;

import com.google.common.collect.Lists;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import org.brylex.hystrix.AccountService;
import org.brylex.hystrix.rest.AccountServiceProxy;

import java.util.List;

public class HystrixTimeout {

    private static class HystrixTimeoutAccountService implements AccountService {

        private final AccountServiceProxy proxy = new AccountServiceProxy();

        private class TimeoutCommand extends HystrixCommand<List<String>> {

            private final String[] accountIds;

            protected TimeoutCommand(final String... accountIds) {
                super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("HystrixTimeoutGroup"))
                        .andCommandPropertiesDefaults(
                                HystrixCommandProperties.Setter()
                                        .withExecutionIsolationThreadTimeoutInMilliseconds(5000)
                        ));
                this.accountIds = accountIds;
            }

            @Override
            protected List<String> run() throws Exception {
                return proxy.get(accountIds);
            }

            @Override
            protected List<String> getFallback() {
                return Lists.newArrayList("FALLBACK");
            }
        }

        @Override
        public List<String> get(final String... accountIds) {
            try {
                return new TimeoutCommand(accountIds).execute();
            } catch (Exception e) {
                throw new RuntimeException("Unable to access AccountsService.", e);
            }
        }
    }

    public static void main(String[] args) {

        final AccountService service = new HystrixTimeoutAccountService();

        System.err.printf("RESULT: [%s].\n", service.get("runepeter"));
    }
}

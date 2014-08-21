package org.brylex.hystrix.collapse;

import com.google.common.collect.Lists;
import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.brylex.hystrix.rest.AccountServiceProxy;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public class RequestCollapser {

    private static class RequestCollapserCommand extends HystrixCollapser<List<String>, String, String> {

        private final String accountId;

        private RequestCollapserCommand(final String accountId) {
            this.accountId = accountId;
        }

        @Override
        public String getRequestArgument() {
            return accountId;
        }

        @Override
        protected HystrixCommand<List<String>> createCommand(Collection<CollapsedRequest<String, String>> requests) {
            return new BatchCommand(requests);
        }

        @Override
        protected void mapResponseToRequests(List<String> batchResponse, Collection<CollapsedRequest<String, String>> requests) {
            int count = 0;
            for (CollapsedRequest<String, String> request : requests) {
                request.setResponse(batchResponse.get(count++));
            }
        }
    }

    private static class BatchCommand extends HystrixCommand<List<String>> {

        private final AccountServiceProxy proxy = new AccountServiceProxy();

        private final Collection<HystrixCollapser.CollapsedRequest<String, String>> requests;

        protected BatchCommand(final Collection<HystrixCollapser.CollapsedRequest<String, String>> requests) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("CollapserGroup"))
                            .andCommandKey(HystrixCommandKey.Factory.asKey("BatchGet"))
            );
            this.requests = requests;
        }

        @Override
        protected List<String> run() throws Exception {

            List<String> list= Lists.newArrayList();
            for (HystrixCollapser.CollapsedRequest<String, String> request : requests) {
                list.add(request.getArgument());
            }

            System.out.println(list.size() + ": " + list.toString());

            String [] array = new String[list.size()];
            array = list.toArray(array);

            return proxy.get(array);
        }
    }

    public static void main(String[] args) throws Exception {

        final int num = 1000;

        final HystrixRequestContext context = HystrixRequestContext.initializeContext();

        final List<Future<String>> futures = Lists.newArrayList();
        for (int i = 0; i < num; i++) {
            Thread.sleep(2);
            futures.add(new RequestCollapserCommand("" + i).queue());
        }

        for (Future<String> future : futures) {
            System.err.printf("RESPONSE: [%s].\n", future.get());
        }

        int count = HystrixRequestLog.getCurrentRequest().getExecutedCommands().size();
        System.out.println("Count: " + count);

        context.shutdown();
    }
}

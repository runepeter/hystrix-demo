package org.brylex.hystrix.retry;

import com.google.common.collect.Lists;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import org.brylex.hystrix.AccountService;
import org.brylex.hystrix.rest.AccountServiceProxy;
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Retry {

    private static class RetryWithDelay implements
        Func1<Observable<? extends Notification<?>>, Observable<?>> {

        private final int maxRetries;
        private final int retryDelayMillis;
        private int retryCount;

        public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
            this.maxRetries = maxRetries;
            this.retryDelayMillis = retryDelayMillis;
            this.retryCount = 0;
        }

        @Override
        public Observable<?> call(Observable<? extends Notification<?>> attempts) {
            return attempts
                .flatMap(new Func1<Notification<?>, Observable<?>>() {
                    @Override
                    public Observable<?> call(Notification errorNotification) {
                        if (++retryCount < maxRetries) {
                            // When this Observable calls onNext, the original
                            // Observable will be retried (i.e. re-subscribed).
                            return Observable.timer(retryDelayMillis,
                                    TimeUnit.MILLISECONDS);
                        }

                        // Max retries hit. Just pass the error along.
                        return Observable.error(errorNotification.getThrowable());
                    }
                });
        }
    }

    private static class HystrixRetryAccountService implements AccountService {

        private final AccountServiceProxy proxy = new AccountServiceProxy();

        private class RetryCommand extends HystrixCommand<List<String>> {

            private final String[] accountIds;

            protected RetryCommand(final String... accountIds) {
                super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RetryGroup")));
                this.accountIds = accountIds;
            }

            @Override
            protected List<String> run() throws Exception {

                System.err.println("run()");

                throw new RuntimeException("provoked failure.");

                //return proxy.get(accountIds);
            }

        }

        @Override
        public List<String> get(final String... accountIds) {

            Observable<List<String>> observable = new RetryCommand(accountIds).observe();

            observable.retryWhen(new RetryWithDelay(3, 2000)).doOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    System.out.println("ERROR IN THE HOUSE!");
                }
            }).subscribe(new Observer<List<String>>() {
                @Override
                public void onCompleted() {
                    System.err.println("complete()");
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("error(" + throwable + ")");
                }

                @Override
                public void onNext(List<String> strings) {
                    System.err.println("next(" + strings + ")");
                }
            });

            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {

        final AccountService service = new HystrixRetryAccountService();

        System.err.printf("RESULT: [%s].\n", service.get("runepeter"));
    }
}

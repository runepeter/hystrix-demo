package org.brylex.hystrix.rest;

import com.google.common.collect.Lists;

import java.util.List;

public class AccountServiceProxy {

    public List<String> get(final String ... accountIds) {

        //sleep();

        final List<String> list = Lists.newArrayList();
        for (String accountId : accountIds) {
            list.add(accountId);
        }

        return list;
    }

    private void sleep() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
    }

}

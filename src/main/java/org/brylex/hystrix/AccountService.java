package org.brylex.hystrix;

import java.util.List;

public interface AccountService {

    List<String> get(String ... accountIds);

}

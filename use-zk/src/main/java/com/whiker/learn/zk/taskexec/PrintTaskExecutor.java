package com.whiker.learn.zk.taskexec;

import com.whiker.learn.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2018/2/20
 */
public class PrintTaskExecutor implements TaskExecutor {
    private static final Logger log = LoggerFactory.getLogger(PrintTaskExecutor.class);

    @Override
    public void exec(byte[] data) {
        log.info("exec {}", new String(data, Util.UTF8));
    }
}

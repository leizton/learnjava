package com.whiker.learn.javabase;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;

/**
 * @author leizton create on 17-3-15.
 */
public class RuntimeExecTest {

    public static void main(String[] args) {
        String proc = ManagementFactory.getRuntimeMXBean().getName();
        final int procId = Integer.parseInt(proc.substring(0, proc.indexOf('@')));
        final Runtime runtime = Runtime.getRuntime();

        runtime.addShutdownHook(new Thread(() -> System.out.println("$" + procId + " shutdown")));

        try {
            /**
             * 不要调用exec执行echo
             *
             * exec一次只执行一条命令
             * runtime.exec("touch a.txt && touch b.txt");
             *     会创建4个文件: a.txt, &&, touch, b.txt
             *     不是只创建a.txt和b.txt
             */

            File f = new File("test.sh");
            FileOutputStream out = new FileOutputStream(f);
            out.write(("kill " + procId + "\ntouch /tmp/mytest/" + procId + ".pid\nrm -f test.sh").getBytes());
            out.close();
            runtime.exec("/bin/bash test.sh");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}

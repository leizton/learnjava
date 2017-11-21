package com.whiker.learn.tianchi.wifiap;

/**
 * @author yiqun.fan@qunar.com create on 16-10-19.
 */
public class WifiAp {

    private int id;
    private String name;
    private int startTime;
    private int[] data;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getData(int time) {
        int index = time - startTime;
        if (index < 0 || index >= data.length) {
            return -1;
        }
        return data[index];
    }

    void setId(int id) {
        this.id = id;
    }

    void setName(String name) {
        this.name = name;
    }

    void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    void setData(int[] data) {
        this.data = data;
    }
}

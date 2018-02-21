package com.whiker.learn.zk;

import com.whiker.learn.common.Util;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ZkClient {
    private static final Logger log = LoggerFactory.getLogger(ZkClient.class);

    private static final int DEFAULT_SESSION_TIMEOUT_MILLIS = 30_000;
    private static final byte[] EMPTY_BYTES = new byte[0];

    public static ZkClient newClient(String server) throws IOException {
        ZooKeeper zk = new ZooKeeper(server, DEFAULT_SESSION_TIMEOUT_MILLIS, event -> log.info("zk event: {}", event));
        return new ZkClient(zk);
    }

    private final ZooKeeper client;

    private ZkClient(ZooKeeper client) {
        this.client = client;
    }

    public Result createNode(String path, boolean ephemeral, boolean sequential) {
        return createNode(path, ephemeral, sequential, EMPTY_BYTES);
    }

    public Result createNode(String path, boolean ephemeral, boolean sequential, String data) {
        return createNode(path, ephemeral, sequential, data.getBytes(Util.UTF8));
    }

    public Result createNode(String path, boolean ephemeral, boolean sequential, byte[] data) {
        CreateMode mode;
        if (ephemeral) {
            mode = sequential ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.EPHEMERAL;
        } else {
            mode = sequential ? CreateMode.PERSISTENT_SEQUENTIAL : CreateMode.PERSISTENT;
        }
        try {
            String realPath = client.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
            return Result.ok(realPath);
        } catch (KeeperException e) {
            if (e.code() == KeeperException.Code.NODEEXISTS) {
                return Result.NodeExist;
            }
            return Result.error(e);
        } catch (Exception e) {
            return Result.error(e);

        }
    }

    public Result getData(String path, Watcher watcher) {
        try {
            Stat stat = new Stat();
            byte[] data = client.getData(path, watcher, stat);
            return Result.ok(stat, data);
        } catch (KeeperException e) {
            if (e.code() == KeeperException.Code.NONODE) {
                return Result.NoNode;
            }
            return Result.error(e);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public Result setData(String path, String data) {
        return setData(path, -1, data.getBytes(Util.UTF8));
    }

    public Result setData(String path, int version, String data) {
        return setData(path, version, data.getBytes(Util.UTF8));
    }

    public Result setData(String path, int version, byte[] data) {
        try {
            Stat stat = client.setData(path, data, version);
            return Result.ok(stat, data);
        } catch (KeeperException e) {
            if (e.code() == KeeperException.Code.NONODE) {
                return Result.NoNode;
            }
            if (e.code() == KeeperException.Code.BADVERSION) {
                return Result.BadVersion;
            }
            return Result.error(e);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public Result getChildren(String path, Watcher watcher, List<String> out) {
        out.clear();
        try {
            List<String> children = client.getChildren(path, watcher);
            out.addAll(children);
            return Result.Ok;
        } catch (KeeperException e) {
            if (e.code() == KeeperException.Code.NONODE) {
                return Result.NoNode;
            }
            return Result.error(e);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public Result delete(String path) {
        return delete(path, -1);
    }

    public Result delete(String path, int version) {
        try {
            client.delete(path, version);
            return Result.Ok;
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public static final class Result {
        private static final Result Ok = new Result(ResultCode.Ok, null, null, null, null);
        private static final Result NodeExist = new Result(ResultCode.NodeExist, null, null, null, null);
        private static final Result NoNode = new Result(ResultCode.NoNode, null, null, null, null);
        private static final Result BadVersion = new Result(ResultCode.BadVersion, null, null, null, null);

        public final ResultCode code;
        public final Exception ex;
        public final String realPath;
        public final Stat stat;
        public final byte[] data;

        private static Result error(Exception ex) {
            return new Result(ResultCode.Error, ex, null, null, null);
        }

        private static Result ok(String realPath) {
            return new Result(ResultCode.Ok, null, realPath, null, null);
        }

        private static Result ok(Stat stat, byte[] data) {
            return new Result(ResultCode.Ok, null, null, stat, data);
        }

        private Result(ResultCode code, Exception ex, String realPath, Stat stat, byte[] data) {
            this.code = code;
            this.ex = ex;
            this.realPath = realPath;
            this.stat = stat;
            this.data = data;
        }

        public String getStringData() {
            return data == null ? "" : new String(data, Util.UTF8);
        }

        @Override
        public String toString() {
            return "Result{" +
                    "code=" + code +
                    ", ex=" + Util.toString(ex) +
                    ", stat=" + (stat == null ? "null" : stat.toString().trim()) +
                    ", data=" + getStringData() +
                    '}';
        }
    }

    public enum ResultCode {
        Ok(1), Error(2), NodeExist(3), NoNode(4), BadVersion(5);

        private int code;

        ResultCode(int code) {
            this.code = code;
        }
    }
}

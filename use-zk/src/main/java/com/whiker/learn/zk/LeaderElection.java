package com.whiker.learn.zk;

public interface LeaderElection {

    String id();

    void elect();

    void exitElection();

    boolean isLeader();

    void subscribeLeaderChangedEvent(LeaderChangeCallback subscriber);

    void unsubscribeLeaderChangedEvent(LeaderChangeCallback subscriber);

    interface LeaderChangeCallback {
        void onLeaderChanged(LeaderChangeEvent event);
    }

    final class LeaderChangeEvent {
        public final boolean isLeader;
        public final boolean isExitElect;

        public LeaderChangeEvent(boolean isLeader, boolean isExitElect) {
            this.isLeader = isLeader;
            this.isExitElect = isExitElect;
        }
    }
}

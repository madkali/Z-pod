package de.danoeh.antennapod.core.storage;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.util.LongIntMap;

import java.util.ArrayList;
import java.util.List;

public class NavDrawerData {
    public final List<DrawerItem> items;
    public final int queueSize;
    public final int numNewItems;
    public final int numDownloadedItems;
    public final LongIntMap feedCounters;
    public final int reclaimableSpace;

    public NavDrawerData(List<DrawerItem> feeds,
                         int queueSize,
                         int numNewItems,
                         int numDownloadedItems,
                         LongIntMap feedIndicatorValues,
                         int reclaimableSpace) {
        this.items = feeds;
        this.queueSize = queueSize;
        this.numNewItems = numNewItems;
        this.numDownloadedItems = numDownloadedItems;
        this.feedCounters = feedIndicatorValues;
        this.reclaimableSpace = reclaimableSpace;
    }

    public abstract static class DrawerItem {
        public enum Type {
            FOLDER, FEED
        }

        public final Type type;
        private int layer;
        public long id;

        public DrawerItem(Type type, long id) {
            this.type = type;
            this.id = id;
        }

        public int getLayer() {
            return layer;
        }

        public void setLayer(int layer) {
            this.layer = layer;
        }

        public abstract String getTitle();

        public abstract int getCounter();
    }

    public static class FolderDrawerItem extends DrawerItem {
        public final List<DrawerItem> children = new ArrayList<>();
        public final String name;
        public boolean isOpen;

        public FolderDrawerItem(String name) {
            super(DrawerItem.Type.FOLDER, (long) name.hashCode() << 20); // Keep IDs >0 but make room for many feeds
            this.name = name;
        }

        public String getTitle() {
            return name;
        }

        public int getCounter() {
            int sum = 0;
            for (DrawerItem item : children) {
                sum += item.getCounter();
            }
            return sum;
        }
    }

    public static class FeedDrawerItem extends DrawerItem {
        public final Feed feed;
        public final int counter;

        public FeedDrawerItem(Feed feed, long id, int counter) {
            super(DrawerItem.Type.FEED, id);
            this.feed = feed;
            this.counter = counter;
        }

        public String getTitle() {
            return feed.getTitle();
        }

        public int getCounter() {
            return counter;
        }
    }
}

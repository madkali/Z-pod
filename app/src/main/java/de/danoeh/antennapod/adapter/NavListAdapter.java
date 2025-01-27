package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.widget.IconTextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.NavDrawerFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * BaseAdapter for the navigation drawer
 */
public class NavListAdapter extends RecyclerView.Adapter<NavListAdapter.Holder>
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int VIEW_TYPE_NAV = 0;
    public static final int VIEW_TYPE_SECTION_DIVIDER = 1;
    private static final int VIEW_TYPE_SUBSCRIPTION = 2;

    /**
     * a tag used as a placeholder to indicate if the subscription list should be displayed or not
     * This tag doesn't correspond to any specific activity.
     */
    public static final String SUBSCRIPTION_LIST_TAG = "SubscriptionList";

    private static List<String> fragmentTags;
    private static String[] titles;

    private final ItemAccess itemAccess;
    private final WeakReference<Activity> activity;
    public boolean showSubscriptionList = true;

    public NavListAdapter(ItemAccess itemAccess, Activity context) {
        this.itemAccess = itemAccess;
        this.activity = new WeakReference<>(context);

        titles = context.getResources().getStringArray(R.array.nav_drawer_titles);
        loadItems();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (UserPreferences.PREF_HIDDEN_DRAWER_ITEMS.equals(key)) {
            loadItems();
        }
    }

    private void loadItems() {
        List<String> newTags = new ArrayList<>(Arrays.asList(NavDrawerFragment.NAV_DRAWER_TAGS));
        List<String> hiddenFragments = UserPreferences.getHiddenDrawerItems();
        newTags.removeAll(hiddenFragments);

        if (newTags.contains(SUBSCRIPTION_LIST_TAG)) {
            // we never want SUBSCRIPTION_LIST_TAG to be in 'tags'
            // since it doesn't actually correspond to a position in the list, but is
            // a placeholder that indicates if we should show the subscription list in the
            // nav drawer at all.
            showSubscriptionList = true;
            newTags.remove(SUBSCRIPTION_LIST_TAG);
        } else {
            showSubscriptionList = false;
        }

        fragmentTags = newTags;
        notifyDataSetChanged();
    }

    public String getLabel(String tag) {
        int index = ArrayUtils.indexOf(NavDrawerFragment.NAV_DRAWER_TAGS, tag);
        return titles[index];
    }

    private Drawable getDrawable(String tag) {
        Activity context = activity.get();
        if (context == null) {
            return null;
        }
        int icon;
        switch (tag) {
            case QueueFragment.TAG:
                icon = R.attr.stat_playlist;
                break;
            case EpisodesFragment.TAG:
                icon = R.attr.feed;
                break;
            case DownloadsFragment.TAG:
                icon = R.attr.av_download;
                break;
            case PlaybackHistoryFragment.TAG:
                icon = R.attr.ic_history;
                break;
            case SubscriptionFragment.TAG:
                icon = R.attr.ic_folder;
                break;
            case AddFeedFragment.TAG:
                icon = R.attr.content_new;
                break;
            default:
                return null;
        }
        TypedArray ta = context.obtainStyledAttributes(new int[] { icon });
        Drawable result = ta.getDrawable(0);
        ta.recycle();
        return result;
    }

    public List<String> getFragmentTags() {
        return Collections.unmodifiableList(fragmentTags);
    }

    @Override
    public int getItemCount() {
        int baseCount = getSubscriptionOffset();
        if (showSubscriptionList) {
            baseCount += itemAccess.getCount();
        }
        return baseCount;
    }

    @Override
    public long getItemId(int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_SUBSCRIPTION) {
            return itemAccess.getItem(position - getSubscriptionOffset()).id;
        } else {
            return -position - 1; // IDs are >0
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (0 <= position && position < fragmentTags.size()) {
            return VIEW_TYPE_NAV;
        } else if (position < getSubscriptionOffset()) {
            return VIEW_TYPE_SECTION_DIVIDER;
        } else {
            return VIEW_TYPE_SUBSCRIPTION;
        }
    }

    public int getSubscriptionOffset() {
        return fragmentTags.size() > 0 ? fragmentTags.size() + 1 : 0;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(activity.get());
        if (viewType == VIEW_TYPE_NAV) {
            return new NavHolder(inflater.inflate(R.layout.nav_listitem, parent, false));
        } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
            return new DividerHolder(inflater.inflate(R.layout.nav_section_item, parent, false));
        } else {
            return new FeedHolder(inflater.inflate(R.layout.nav_listitem, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        int viewType = getItemViewType(position);

        holder.itemView.setOnCreateContextMenuListener(null);
        if (viewType == VIEW_TYPE_NAV) {
            bindNavView(getLabel(fragmentTags.get(position)), position, (NavHolder) holder);
        } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
            bindSectionDivider((DividerHolder) holder);
        } else {
            int itemPos = position - getSubscriptionOffset();
            NavDrawerData.DrawerItem item = itemAccess.getItem(itemPos);
            bindListItem(item, (FeedHolder) holder);
            if (item.type == NavDrawerData.DrawerItem.Type.FEED) {
                bindFeedView((NavDrawerData.FeedDrawerItem) item, (FeedHolder) holder);
                holder.itemView.setOnCreateContextMenuListener(itemAccess);
            } else {
                bindFolderView((NavDrawerData.FolderDrawerItem) item, (FeedHolder) holder);
            }
        }
        if (viewType != VIEW_TYPE_SECTION_DIVIDER) {
            TypedValue typedValue = new TypedValue();

            activity.get().getTheme().resolveAttribute(itemAccess.isSelected(position)
                    ? R.attr.drawer_activated_color : android.R.attr.windowBackground, typedValue, true);
            holder.itemView.setBackgroundResource(typedValue.resourceId);

            holder.itemView.setOnClickListener(v -> itemAccess.onItemClick(position));
            holder.itemView.setOnLongClickListener(v -> itemAccess.onItemLongClick(position));
        }
    }

    private void bindNavView(String title, int position, NavHolder holder) {
        Activity context = activity.get();
        if (context == null) {
            return;
        }
        holder.title.setText(title);

        // reset for re-use
        holder.count.setVisibility(View.GONE);
        holder.count.setOnClickListener(null);

        String tag = fragmentTags.get(position);
        if (tag.equals(QueueFragment.TAG)) {
            int queueSize = itemAccess.getQueueSize();
            if (queueSize > 0) {
                holder.count.setText(NumberFormat.getInstance().format(queueSize));
                holder.count.setVisibility(View.VISIBLE);
            }
        } else if (tag.equals(EpisodesFragment.TAG)) {
            int unreadItems = itemAccess.getNumberOfNewItems();
            if (unreadItems > 0) {
                holder.count.setText(NumberFormat.getInstance().format(unreadItems));
                holder.count.setVisibility(View.VISIBLE);
            }
        } else if (tag.equals(SubscriptionFragment.TAG)) {
            int sum = itemAccess.getFeedCounterSum();
            if (sum > 0) {
                holder.count.setText(NumberFormat.getInstance().format(sum));
                holder.count.setVisibility(View.VISIBLE);
            }
        } else if (tag.equals(DownloadsFragment.TAG) && UserPreferences.isEnableAutodownload()) {
            int epCacheSize = UserPreferences.getEpisodeCacheSize();
            // don't count episodes that can be reclaimed
            int spaceUsed = itemAccess.getNumberOfDownloadedItems()
                    - itemAccess.getReclaimableItems();

            if (epCacheSize > 0 && spaceUsed >= epCacheSize) {
                holder.count.setText("{md-disc-full 150%}");
                Iconify.addIcons(holder.count);
                holder.count.setVisibility(View.VISIBLE);
                holder.count.setOnClickListener(v ->
                        new AlertDialog.Builder(context)
                            .setTitle(R.string.episode_cache_full_title)
                            .setMessage(R.string.episode_cache_full_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> { })
                            .show()
                );
            }
        }

        holder.image.setImageDrawable(getDrawable(fragmentTags.get(position)));
    }

    private void bindSectionDivider(DividerHolder holder) {
        Activity context = activity.get();
        if (context == null) {
            return;
        }

        if (UserPreferences.getSubscriptionsFilter().isEnabled() && showSubscriptionList) {
            holder.itemView.setEnabled(true);
            holder.feedsFilteredMsg.setText("{md-info-outline} "
                    + context.getString(R.string.subscriptions_are_filtered));
            Iconify.addIcons(holder.feedsFilteredMsg);
            holder.feedsFilteredMsg.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setEnabled(false);
            holder.feedsFilteredMsg.setVisibility(View.GONE);
        }
    }

    private void bindListItem(NavDrawerData.DrawerItem item, FeedHolder holder) {
        if (item.getCounter() > 0) {
            holder.count.setVisibility(View.VISIBLE);
            holder.count.setText(NumberFormat.getInstance().format(item.getCounter()));
        } else {
            holder.count.setVisibility(View.GONE);
        }
        holder.title.setText(item.getTitle());
        int padding = (int) (activity.get().getResources().getDimension(R.dimen.thumbnail_length_navlist) / 2);
        holder.itemView.setPadding(item.getLayer() * padding, 0, 0, 0);
    }

    private void bindFeedView(NavDrawerData.FeedDrawerItem drawerItem, FeedHolder holder) {
        Feed feed = drawerItem.feed;
        Activity context = activity.get();
        if (context == null) {
            return;
        }

        Glide.with(context)
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                    .placeholder(R.color.light_gray)
                    .error(R.color.light_gray)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .fitCenter()
                    .dontAnimate())
                .into(holder.image);

        if (feed.hasLastUpdateFailed()) {
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) holder.title.getLayoutParams();
            p.addRule(RelativeLayout.LEFT_OF, R.id.itxtvFailure);
            holder.failure.setVisibility(View.VISIBLE);
        } else {
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) holder.title.getLayoutParams();
            p.addRule(RelativeLayout.LEFT_OF, R.id.txtvCount);
            holder.failure.setVisibility(View.GONE);
        }
    }

    private void bindFolderView(NavDrawerData.FolderDrawerItem folder, FeedHolder holder) {
        Activity context = activity.get();
        if (context == null) {
            return;
        }
        if (folder.isOpen) {
            holder.count.setVisibility(View.GONE);
        }
        Glide.with(context).clear(holder.image);
        holder.image.setImageResource(ThemeUtils.getDrawableFromAttr(context, R.attr.ic_folder));
        holder.failure.setVisibility(View.GONE);
    }

    static class Holder extends RecyclerView.ViewHolder {
        public Holder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class DividerHolder extends Holder {
        final TextView feedsFilteredMsg;

        public DividerHolder(@NonNull View itemView) {
            super(itemView);
            feedsFilteredMsg = itemView.findViewById(R.id.nav_feeds_filtered_message);
        }
    }

    static class NavHolder extends Holder {
        final ImageView image;
        final TextView title;
        final TextView count;

        public NavHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgvCover);
            title = itemView.findViewById(R.id.txtvTitle);
            count = itemView.findViewById(R.id.txtvCount);
        }
    }

    static class FeedHolder extends Holder {
        final ImageView image;
        final TextView title;
        final IconTextView failure;
        final TextView count;

        public FeedHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgvCover);
            title = itemView.findViewById(R.id.txtvTitle);
            failure = itemView.findViewById(R.id.itxtvFailure);
            count = itemView.findViewById(R.id.txtvCount);
        }
    }

    public interface ItemAccess extends View.OnCreateContextMenuListener {
        int getCount();

        NavDrawerData.DrawerItem getItem(int position);

        boolean isSelected(int position);

        int getQueueSize();

        int getNumberOfNewItems();

        int getNumberOfDownloadedItems();

        int getReclaimableItems();

        int getFeedCounterSum();

        void onItemClick(int position);

        boolean onItemLongClick(int position);

        @Override
        void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo);
    }

}

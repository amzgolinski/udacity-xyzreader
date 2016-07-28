package com.example.xyzreader.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

/**
 * An activity representing a list of Articles. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
  LoaderManager.LoaderCallbacks<Cursor> {

  public static final String LOG_TAG =
    ArticleListActivity.class.getSimpleName();

  private Toolbar mToolbar;
  private SwipeRefreshLayout mSwipeRefreshLayout;
  private RecyclerView mRecyclerView;
  private boolean mIsRefreshing = false;
  private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(
        intent.getAction())) {

        mIsRefreshing =
          intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);

        updateRefreshingUI();
      }
    }
  };

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newAllArticlesInstance(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_article_list);

    mToolbar = (Toolbar) findViewById(R.id.toolbar);

    mSwipeRefreshLayout =
      (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

    mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

    getLoaderManager().initLoader(0, null, this);

    if (savedInstanceState == null) {
      refresh();
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mRecyclerView.setAdapter(null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    Adapter adapter = new Adapter(cursor, this);
    adapter.setHasStableIds(true);
    mRecyclerView.setAdapter(adapter);

    RecyclerView.LayoutManager mLayoutManager =
      new LinearLayoutManager(getApplicationContext());

    mRecyclerView.setLayoutManager(mLayoutManager);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());

    Drawable separator = getResources()
      .getDrawable(R.drawable.padded_divider, getTheme());

    mRecyclerView.addItemDecoration(
      new DividerItemDecoration(this, separator, LinearLayoutManager.VERTICAL)
    );
  }

  @Override
  protected void onStart() {
    super.onStart();
    registerReceiver(
      mRefreshingReceiver,
      new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE)
    );
  }

  @Override
  protected void onStop() {
    super.onStop();
    unregisterReceiver(mRefreshingReceiver);
  }

  private void refresh() {
    startService(new Intent(this, UpdaterService.class));
  }
  private void updateRefreshingUI() {
    mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
  }

  private class Adapter extends RecyclerView.Adapter<ViewHolder> {
    private Cursor mCursor;
    private Context mContext;

    public Adapter(Cursor cursor, Context context) {
      super();
      mCursor = cursor;
      mContext = context;
    }

    @Override
    public int getItemCount() {
      return mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
      mCursor.moveToPosition(position);
      return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

      View view = getLayoutInflater().inflate(
        R.layout.list_item_article_relative,
        parent,
        false
      );

      final ViewHolder viewHolder = new ViewHolder(view);
      final Activity activity = (Activity) mContext;


      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

          Intent intent = new Intent(Intent.ACTION_VIEW,
            ItemsContract.Items.buildItemUri(
              getItemId(viewHolder.getAdapterPosition())));

          Bundle transition = ActivityOptions.makeSceneTransitionAnimation(
            activity,
            viewHolder.thumbnailView,
            "image")
            .toBundle();

          startActivity(intent, transition);
        }
      });
      return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

      mCursor.moveToPosition(position);
      holder.title.setText(mCursor.getString(ArticleLoader.Query.TITLE));

      String date = DateUtils.getRelativeTimeSpanString(
        mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
        System.currentTimeMillis(),
        DateUtils.HOUR_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
      ).toString();

      String author = getResources().getString(
        R.string.article_author,
        mCursor.getString(ArticleLoader.Query.AUTHOR)
      );

      holder.author.setText(author);
      holder.date.setText(date);

      Picasso.with(mContext)
        .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
        .error(R.drawable.ic_do_not_disturb_black_24dp)
        .into(holder.thumbnailView);

    }

  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ImageView thumbnailView;
    public TextView title;
    public TextView author;
    public TextView date;

    public ViewHolder(View view) {
      super(view);
      thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
      title = (TextView) view.findViewById(R.id.article_title);
      author = (TextView) view.findViewById(R.id.article_author);
      date = (TextView) view.findViewById(R.id.article_date);
    }
  }
}

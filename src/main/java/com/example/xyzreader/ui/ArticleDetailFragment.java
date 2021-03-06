package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
  LoaderManager.LoaderCallbacks<Cursor> {

  // Constants
  public static final String ARG_ITEM_ID = "item_id";
  private static final String LOG_TAG =
    ArticleDetailFragment.class.getSimpleName();
  private static final int MUTED_COLOR = 0xFF333333;


  // Views
  private ImageView mPhotoView;
  private View mRootView;
  private NestedScrollView mScrollView;

  private Cursor mCursor;
  private long mItemId;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ArticleDetailFragment() {
  }

  public static ArticleDetailFragment newInstance(long itemId) {
    Bundle arguments = new Bundle();
    arguments.putLong(ARG_ITEM_ID, itemId);
    ArticleDetailFragment fragment = new ArticleDetailFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      mItemId = getArguments().getLong(ARG_ITEM_ID);
    }

    setHasOptionsMenu(true);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // In support library r8, calling initLoader for a fragment in a
    // FragmentPagerAdapter in the fragment's onCreate may cause the same
    // LoaderManager to be dealt to multiple fragments because their mIndex
    // is -1 (haven't been added to the activity yet). Thus, we do this in
    // onActivityCreated.
    getLoaderManager().initLoader(0, null, this);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    mRootView = inflater.inflate(
      R.layout.fragment_article_detail_collapsing,
      container,
      false);

    mScrollView =
      (NestedScrollView) mRootView.findViewById(R.id.scrollview);

    // Find the toolbar view inside the activity layout
    Toolbar toolbar = (Toolbar) mRootView.findViewById(R.id.app_bar);
    if (toolbar != null) {
      toolbar.setNavigationOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          getActivity().onBackPressed();
        }
      });
    }

    mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

    mRootView.findViewById(R.id.share_fab).setOnClickListener(
      new View.OnClickListener() {

      @Override
      public void onClick(View view) {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder
          .from(getActivity())
          .setType("text/plain")
          .setText("Some sample text")
          .getIntent(), getString(R.string.action_share)));
      }
    });

    bindViews();
    return mRootView;
  }

  private void bindViews() {

    if (mRootView == null) {
      return;
    }

    TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
    TextView dateView = (TextView) mRootView.findViewById(R.id.article_date);
    TextView authorView
        = (TextView) mRootView.findViewById(R.id.article_author);

    TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

    if (mCursor != null) {

      mRootView.setAlpha(0);
      mRootView.setVisibility(View.VISIBLE);
      mRootView.animate().alpha(1);

      titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

      String date = DateUtils.getRelativeTimeSpanString(
        mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
        System.currentTimeMillis(),
        DateUtils.HOUR_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
      ).toString();

      dateView.setText(
        String.format(getResources().getString(R.string.article_date), date)
      );

      authorView.setText(mCursor.getString(ArticleLoader.Query.AUTHOR));
      bodyView.setText(
        Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY))
      );

      ImageLoaderHelper.getInstance(getActivity()).getImageLoader().get(
        mCursor.getString(ArticleLoader.Query.PHOTO_URL),
        new ImageLoader.ImageListener() {
          @Override
          public void onResponse(ImageLoader.ImageContainer imageContainer,
                                 boolean b) {
            Bitmap bitmap = imageContainer.getBitmap();
            if (bitmap != null) {
              Palette p = Palette.from(bitmap).generate();
              mPhotoView.setImageBitmap(imageContainer.getBitmap());
              mRootView.findViewById(R.id.article_header)
                  .setBackgroundColor(p.getDarkMutedColor(MUTED_COLOR));
            }
          }

          @Override
          public void onErrorResponse(VolleyError volleyError) {

          }
        });
    } else {
      mRootView.setVisibility(View.GONE);
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    if (!isAdded()) {
      if (cursor != null) {
        cursor.close();
      }
      return;
    }

    mCursor = cursor;
    if (mCursor != null && !mCursor.moveToFirst()) {
      Log.e(LOG_TAG, "Error reading item detail cursor");
      mCursor.close();
      mCursor = null;
    }

    bindViews();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> cursorLoader) {
    mCursor = null;
    bindViews();
  }

}

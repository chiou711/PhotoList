/*
 * Copyright (c) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.photolist.ui.options.browse_category;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.view.View;

import com.cw.photolist.R;
import com.cw.photolist.data.DbData;
import com.cw.photolist.data.PhotoContract;
import com.cw.photolist.model.Video;
import com.cw.photolist.model.VideoCursorMapper;
import com.cw.photolist.presenter.CardPresenter_browse;
import com.cw.photolist.ui.options.setting.SettingsActivity;
import com.cw.photolist.ui.photo.AutoPhotoAct;
import com.cw.photolist.ui.photo.ManualPhotoFragment;
import com.cw.photolist.utility.Pref;

/*
 * BrowseCategoryFragment shows a grid of videos that can be scrolled vertically.
 */
public class BrowseCategoryFragment extends VerticalGridSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int NUM_COLUMNS = 5;
    private CursorObjectAdapter mVideoCursorAdapter;
    private static final int ALL_VIDEOS_LOADER = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // parameter -1 is used for hiding row number in card view
        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter_browse(getActivity(),-1));

        mVideoCursorAdapter.setMapper(new VideoCursorMapper());
        setAdapter(mVideoCursorAdapter);

        setTitle(getString(R.string.category_grid_view_title));

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }
        setupFragment();
    }

    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        getLoaderManager().initLoader(ALL_VIDEOS_LOADER, null, this);

        // After 500ms, start the animation to transition the cards into view.
        new Handler().postDelayed(new Runnable() {
            public void run() {
                startEntranceTransition();
            }
        }, 500);

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(getActivity(), SearchActivity.class);
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                startActivity(intent, bundle);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                PhotoContract.VideoEntry.CONTENT_URI,
                null, // projection
                null, // selection
                null, // selection clause
                null  // sort order
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == ALL_VIDEOS_LOADER && cursor != null && cursor.moveToFirst()) {
            mVideoCursorAdapter.changeCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                System.out.println("VerticalGridFragment /  _onItemClicked");
                Video video = (Video) item;

                // auto play
                if (Pref.isAutoPlay(getActivity())){
                    Intent intent = new Intent(getActivity(), AutoPhotoAct.class);
                    int pos = DbData.getCursorPositionById(getContext(), (int) video.id);
                    intent.putExtra("PHOTO_POSITION", pos);
                    startActivity(intent);

                } else {
                    // manual play
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            int pos = DbData.getCursorPositionById(getContext(), (int) video.id);
                            ManualPhotoFragment fragment = new ManualPhotoFragment();
                            final Bundle args = new Bundle();
                            args.putInt("PHOTO_POSITION", pos);
                            fragment.setArguments(args);
                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                            transaction.replace(R.id.vertical_grid_fragment, fragment,"photo").addToBackStack("photo_stack").commit();
                        }
                    });
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
        }
    }
}
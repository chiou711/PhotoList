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

package com.cw.photolist.presenter;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.photolist.utility.Pref;
import com.cw.photolist.R;
import com.cw.photolist.define.Define;
import com.cw.photolist.model.Video;
import com.cw.photolist.ui.MainFragment;
import com.cw.photolist.ui.VideoDetailsActivity;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {
    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;
//    private Drawable mDefaultCardImage;
    FragmentActivity act;
    boolean isGotDuration;
    String acquiredDuration;
    String duration;

    public  CardPresenter(){}
    int row_id;

    public CardPresenter(FragmentActivity main_act,int rowId){
        act = main_act;
        row_id = rowId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mDefaultBackgroundColor = ContextCompat.getColor(parent.getContext(), R.color.default_background);
        mSelectedBackgroundColor = ContextCompat.getColor(parent.getContext(), R.color.selected_background);
//        mDefaultCardImage = parent.getResources().getDrawable(R.drawable.movie, null);

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Video video = (Video) item;

        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(video.title);
        ((TextView)cardView.findViewById(R.id.title_text)).setLines(1);// setMaxLines(5);
        ((TextView)cardView.findViewById(R.id.title_text)).setHeight(40);

        // get duration
        if(Pref.isShowDuration(act)){
            isGotDuration = false;

            //wait for buffering
            int time_out_count = 0;
            while ((!isGotDuration) && time_out_count< 10)
            {
                try {
                    Thread.sleep(Define.DEFAULT_DELAY_GET_DURATION);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                time_out_count++;
            }
            duration = acquiredDuration;
        } else { // no duration
            duration = "";
        }

        // set position info
        int row_number = row_id;
        String positionInfo;
        int rowLength = MainFragment.getRowInfoByVideoId(video.id);
        int linkPosInRow = MainFragment.getLinkPositionInRowByVideoId(video.id);

        String linkNumberOfRowLinks = "    (" + linkPosInRow + "/" +
                rowLength + ")    ";
        if(row_number == -1) {
            // duration + link number of row links
            positionInfo = duration +linkNumberOfRowLinks;

        } else {
            //  duration + link number of row links + playlist number
            positionInfo = cardView.getContext().getResources().getString(R.string.current_list_title);
            positionInfo = duration + linkNumberOfRowLinks + positionInfo + row_number;
        }
        cardView.setContentText(positionInfo);

        TextView positionText = ((TextView)cardView.findViewById(R.id.content_text));
        positionText.setTextColor(cardView.getContext().getResources().getColor(R.color.category_text));
        positionText.setGravity(Gravity.RIGHT);
        positionText.setHeight(40);

        if (video.cardImageUrl != null) {
            // Set card size from dimension resources.
            Resources res = cardView.getResources();
            int width = res.getDimensionPixelSize(R.dimen.card_width);
            int height = res.getDimensionPixelSize(R.dimen.card_height);
            cardView.setMainImageDimensions(width, height);

            // original
//            Glide.with(cardView.getContext())
//                    .load(video.cardImageUrl)
//                    .apply(RequestOptions.errorOf(mDefaultCardImage))
//                    .into(cardView.getMainImageView());

            // with onResourceReady / onLoadFailed
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .placeholder(R.drawable.movie)
                    .error(R.drawable.movie)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .priority(Priority.HIGH);

            Glide.with(cardView.getContext())
                    .asBitmap()
                    .load(video.cardImageUrl)
                    .apply(options)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(
                                Bitmap resource,
                                Transition<? super Bitmap> transition) {
//                            Drawable mDrawable = new BitmapDrawable(act.getResources(), resource);
                            Drawable drawable = getScaledDrawable(resource,0.5f,0.5f);
                            cardView.setMainImage(drawable);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            System.out.println("CardPresenter / _onLoadFailed");
                            cardView.setMainImage(act.getResources().getDrawable(R.drawable.movie));
                        }
                    });
        }

        // card view long click listener: launch VideoDetailsActivity
        cardView.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                System.out.println("CardPresenter / onLongClick");
                if (item instanceof Video) {
                    Video video = (Video) item;
                    Intent intent = new Intent(act, VideoDetailsActivity.class);
                    intent.putExtra(VideoDetailsActivity.VIDEO, video);

                    act.runOnUiThread(new Runnable() {
                        public void run() {
                            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    act,
                                    ((ImageCardView) viewHolder.view).getMainImageView(),
                                    VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                            act.startActivityForResult(intent, MainFragment.VIDEO_DETAILS_INTENT, bundle);
                        }
                    });
                }
                return true;
            }
        });
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }

    // Get scaled drawable
    private Drawable getScaledDrawable(Bitmap resource,float ratioX,float ratioY){
        int width = resource.getWidth();
        int height = resource.getHeight();

        // create a matrix for the manipulation
        Matrix matrix = new Matrix();

        // resize the bit map
        matrix.postScale(ratioX, ratioY);

        // recreate the new Bitmap
        Bitmap bitmap = Bitmap.createBitmap(resource, 0, 0, width, height, matrix, false);
        Drawable drawable = new BitmapDrawable(act.getResources(), bitmap);

        return drawable;
    }

}

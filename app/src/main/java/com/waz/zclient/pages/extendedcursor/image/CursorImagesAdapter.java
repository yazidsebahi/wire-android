/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.pages.extendedcursor.image;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.waz.zclient.R;
import com.waz.zclient.messages.parts.*;

class CursorImagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int VIEW_TYPE_CAMERA = 0;
    private static final int VIEW_TYPE_GALLERY = 1;

    private Cursor cursor;
    private CursorImagesLayout.Callback callback;
    private AdapterCallback adapterCallback;
    private CameraViewHolder cameraViewHolder;
    private CursorCameraLayout.Callback cameraCallback = new CursorCameraLayout.Callback() {
        @Override
        public void openCamera() {
            if (callback != null) {
                callback.openCamera();
            }
        }

        @Override
        public void openVideo() {
            if (callback != null) {
                callback.openVideo();
            }
        }

        @Override
        public void onCameraPreviewAttached() {
            adapterCallback.onCameraPreviewAttached();
        }

        @Override
        public void onCameraPreviewDetached() {
            adapterCallback.onCameraPreviewDetached();
        }

        @Override
        public void onPictureTaken(byte[] imageData, boolean isMirrored) {
            if (callback != null) {
                callback.onPictureTaken(imageData, isMirrored);
            }
        }
    };

    private boolean closed = false;
    private ContentObserver observer = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (!closed) {
                load();
            }
        }
    };
    private final ContentResolver resolver;

    CursorImagesAdapter(Context context, AdapterCallback adapterCallback) {
        this.resolver = context.getContentResolver();
        this.adapterCallback = adapterCallback;

        load();
        resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, observer);
    }

    private void load() {
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... params) {
                Cursor c = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
                c.moveToLast(); // force cursor loading and move to last, as we are displaying images in reverse order
                return c;
            }

            @Override
            protected void onPostExecute(Cursor c) {
                if (closed) {
                    c.close();
                } else {
                    if (cursor != null) {
                        cursor.close();
                    }
                    cursor = c;
                    notifyDataSetChanged();
                }
            }
        } .execute();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_CAMERA) {
            cameraViewHolder = new CameraViewHolder(inflater.inflate(R.layout.item_camera_cursor, parent, false));
            cameraViewHolder.getLayout().setCallback(cameraCallback);
            return cameraViewHolder;
        } else {
            CursorGalleryItem item = (CursorGalleryItem)inflater.inflate(R.layout.item_cursor_gallery, parent, false);
            return new GalleryItemViewHolder(item);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_GALLERY) {
            cursor.moveToPosition(cursor.getCount() - position);
            ((GalleryItemViewHolder) holder).bind(
                cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)),
                callback
            );
        }
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 1 : cursor.getCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_CAMERA : VIEW_TYPE_GALLERY;
    }

    void close() {
        closed = true;
        if (cameraViewHolder != null) {
            cameraViewHolder.getLayout().onClose();
        }

        if (cursor != null) {
            cursor.close();
            cursor = null;
            notifyDataSetChanged();
        }

        resolver.unregisterContentObserver(observer);
    }

    private static class CameraViewHolder extends RecyclerView.ViewHolder {

        public CursorCameraLayout getLayout() {
            return (CursorCameraLayout) itemView;
        }

        CameraViewHolder(View itemView) {
            super(itemView);
        }
    }

    public void setCallback(CursorImagesLayout.Callback callback) {
        this.callback = callback;
    }


    interface AdapterCallback {
        void onCameraPreviewDetached();
        void onCameraPreviewAttached();
    }
}


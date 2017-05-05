package com.project.coursera.dailyselfie;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ignacio on 28-04-2017.
 */

class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private Context mContext;
    private GalleryItemCursor mCursor;
    private OnViewImageListener mViewImageCallback;
    private ActionMode.Callback mActionModeCallback;
    ActionMode mActionMode;
    private SparseBooleanArray mSelectedItems = new SparseBooleanArray();

    GalleryAdapter(Context context,
                   OnViewImageListener viewImageCallback,
                   ActionMode.Callback actionModeCallback,
                   @Nullable ArrayList<Integer> selectedItems) {
        mContext = context;
        mViewImageCallback = viewImageCallback;
        mActionModeCallback = actionModeCallback;
        mCursor = new GalleryItemCursor(null);
        if (selectedItems != null) {
            for (Integer position :
                    selectedItems) {
                mSelectedItems.put(position, true);
            }
            if (mActionMode == null && mSelectedItems.size() > 0) {
                mActionMode = ((AppCompatActivity) mContext).startSupportActionMode(mActionModeCallback);
            }
        }
    }

    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View galleryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gallery_item_layout, parent, false);

        return new ViewHolder(galleryView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        mCursor.moveToPosition(position);

        // Find the gallery item
        final String photoPath = mCursor.getImagePath();
        long photoID;
        try {
            photoID = mCursor.getImageID();
        } catch (NumberFormatException e) {
            photoID = 0;
        }

        // Either create a loader if this view hasn't got one yet, or cancel the existing load so we
        // can start loading the new image
        if (viewHolder.mBitmapLoaderTask == null) {
            viewHolder.mBitmapLoaderTask = new BitmapLoaderTask(mContext,
                    viewHolder.mImageView,
                    viewHolder.mProgressIcon);
        } else if (viewHolder.mBitmapLoaderTask.getStatus() == AsyncTask.Status.RUNNING
                ||
                viewHolder.mBitmapLoaderTask.getStatus() == AsyncTask.Status.PENDING
                ||
                viewHolder.mBitmapLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
            viewHolder.mBitmapLoaderTask.cancel(true);
            viewHolder.mBitmapLoaderTask = null;
            viewHolder.mProgressIcon.setVisibility(View.VISIBLE);
            viewHolder.mBitmapLoaderTask = new BitmapLoaderTask(mContext,
                    viewHolder.mImageView,
                    viewHolder.mProgressIcon);
        }

        viewHolder.mItemState.setActivated(mSelectedItems.get(position, false));
        viewHolder.mBitmapLoaderTask.execute(photoID);

        if (mActionMode != null) {
            updateActionTitle();
        }

        viewHolder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActionMode == null)
                    mViewImageCallback.viewImage(photoPath);
                else
                    updateSelection(viewHolder.getAdapterPosition(), viewHolder.mItemState);
            }
        });
        viewHolder.mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mActionMode == null) {
                    mActionMode = ((AppCompatActivity) mContext).startSupportActionMode(mActionModeCallback);
                    updateSelection(viewHolder.getAdapterPosition(), viewHolder.mItemState);
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    // Replace the existing cursor in the cursor wrapper
    Cursor swapCursor(Cursor cursor) {
        if (mCursor.getWrappedCursor() == cursor) {
            return null;
        }
        Cursor oldCursor = mCursor.getWrappedCursor();
        mCursor = new GalleryItemCursor(cursor);
        if (cursor != null) {
            notifyDataSetChanged();
        }
        return oldCursor;
    }

    // Called when a user clicks an item in action mode
    private void updateSelection(int index, View stateIndicator) {
        stateIndicator.setActivated(!stateIndicator.isActivated());
        toggleSelection(index);
        updateActionTitle();
    }

    // Set the action title based on the number of items selected
    private void updateActionTitle() {
        int selectedItemCount = this.getSelectedItemCount();
        if (selectedItemCount == this.getItemCount()) {
            String title = mContext.getString(
                    R.string.selected_all);
            mActionMode.setTitle(title);
        }
        else if (selectedItemCount > 0) {
            String title = mContext.getString(
                    R.string.selected_count,
                    selectedItemCount);
            mActionMode.setTitle(title);
        } else {
            mActionMode.finish();
        }
    }

    // Toggle the selection state of the view at the given position
    private void toggleSelection(int pos) {
        if (mSelectedItems.get(pos, false)) {
            mSelectedItems.delete(pos);
        }
        else {
            mSelectedItems.put(pos, true);
        }
    }

    // Mark the item as selected if it isn't already, and return whether or not we selected it
    private boolean selectItem(int pos) {
        if (!mSelectedItems.get(pos, false)) {
            mSelectedItems.put(pos, true);
            return true;
        }
        return false;
    }

    // Clear all currently selected views
    void clearSelections(RecyclerView galleryView) {
        for(int i = 0; i < mSelectedItems.size(); i++) {
            int pos = mSelectedItems.keyAt(i);
            if (mSelectedItems.get(pos, false)) {
                ViewHolder viewHolder = (ViewHolder) galleryView.findViewHolderForAdapterPosition(pos);
                if (viewHolder != null) {
                    viewHolder.mItemState.setActivated(false);
                }
            }
        }
        mSelectedItems.clear();
    }

    // Select ALL of the items
    void selectAll(RecyclerView galleryView) {
        for (int pos = 0; pos < this.getItemCount(); pos++) {
            if (selectItem(pos)) {
                ViewHolder viewHolder = (ViewHolder) galleryView.findViewHolderForAdapterPosition(pos);
                if (viewHolder != null) {
                    viewHolder.mItemState.setActivated(true);
                }
            }
        }
        updateActionTitle();
    }

    // Get the number of items currently selected
    int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    // Return a list of all items marked as selected
    ArrayList<Integer> getSelectedItems() {
        ArrayList<Integer> items =
                new ArrayList<>(mSelectedItems.size());
        for (int i = 0; i < mSelectedItems.size(); i++) {
            items.add(mSelectedItems.keyAt(i));
        }
        return items;
    }

    // Delete item at the given position
    String[] getImagePaths(List<Integer> positions) {
        List<String> photoPaths = new ArrayList<>();
        // for loop to delete items
        for (int i = positions.size() - 1; i >= 0; i--) {
            mCursor.moveToPosition(positions.get(i));
            Uri photoUri = Uri.parse(mCursor.getImagePath());
            photoPaths.add(photoUri.getPath());
        }
        return photoPaths.toArray(new String[photoPaths.size()]);
    }

    // ViewHolder implementation to reduce view creation/deletion
    static class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout mProgressIcon;
        RelativeLayout mItemState;
        AppCompatImageView mImageView;
        BitmapLoaderTask mBitmapLoaderTask;

        ViewHolder(View galleryItem) {
            super(galleryItem);
            mImageView = (AppCompatImageView) galleryItem.findViewById(R.id.image);
            mProgressIcon = (RelativeLayout) galleryItem.findViewById(R.id.progress_icon);
            mItemState = (RelativeLayout) galleryItem.findViewById(R.id.gallery_item_state);
        }
    }
}

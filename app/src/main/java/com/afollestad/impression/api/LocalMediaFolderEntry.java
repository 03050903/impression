package com.afollestad.impression.api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.provider.MediaStore;

import com.afollestad.impression.R;
import com.afollestad.impression.accounts.Account;
import com.afollestad.impression.accounts.AccountDbUtil;
import com.afollestad.impression.media.MediaAdapter;
import com.afollestad.impression.utils.PrefUtils;

import java.io.File;
import java.util.List;

import rx.Single;
import rx.functions.Func1;

public class LocalMediaFolderEntry extends LocalMediaEntry implements MediaFolderEntry {

    public static final String OVERVIEW_PATH = "OVERVIEW";

    public LocalMediaFolderEntry() {
    }


    @SuppressLint("SwitchIntDef")
    public static String getSortQueryForThumb(@MediaAdapter.SortMode int from) {
        switch (from) {
            case MediaAdapter.SORT_TAKEN_DATE_ASC:
                return "MAX(" + MediaStore.Images.Media.DATE_TAKEN + ") ASC";
            case MediaAdapter.SORT_TAKEN_DATE_DESC:
                return "MAX(" + MediaStore.Images.Media.DATE_TAKEN + ") DESC";
            case MediaAdapter.SORT_NAME_ASC:
                return "MAX(" + MediaStore.Images.Media.DISPLAY_NAME + ") ASC";
            default:
                return "MAX(" + MediaStore.Images.Media.DISPLAY_NAME + ") DESC";
        }
    }

    @Override
    public String toString() {
        return "MediaFolderEntry{" +
                "_id=" + _id +
                ", _data='" + _data + '\'' +
                ", _size=" + _size +
                ", title='" + title + '\'' +
                ", _displayName='" + _displayName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", dateAdded=" + dateAdded +
                ", dateTaken=" + dateTaken +
                ", dateModified=" + dateModified +
                ", bucketDisplayName='" + bucketDisplayName + '\'' +
                ", bucketId='" + bucketId + '\'' +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    @Override
    public long getId() {
        return bucketId.hashCode();
    }

    @Override
    public String firstData() {
        return _data;
    }

    @Override
    public String getData() {
        return new File(_data).getParent();
    }

    @Override
    public long getSize() {
        //TODO - like Cabinet, query later
        return -1;
    }

    @Override
    public int getWidth() {
        return -1;
    }

    @Override
    public int getHeight() {
        return -1;
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public void delete(final Activity context) {
        List<MediaEntry> mediaEntries = AccountDbUtil.getCurrentAccount(context).flatMap(new Func1<Account, Single<List<MediaEntry>>>() {
            @Override
            public Single<List<MediaEntry>> call(Account account) {
                //noinspection ResourceType
                return account.getEntries(context, getData(), false, PrefUtils.getFilterMode(context), -1);
            }
        }).toBlocking().value();
        for (MediaEntry entry : mediaEntries) {
            entry.delete(context);
        }
    }

    @Override
    public String getDisplayName(Context context) {
        if (getData().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            return context.getString(R.string.internal_storage);
        }
        return bucketDisplayName;
    }

    @Override
    public String getMimeType() {
        return "";
    }

    @Override
    public int hashCode() {
        return 31 * getData().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LocalMediaFolderEntry && getData().equals(((LocalMediaFolderEntry) o).getData());
    }
}

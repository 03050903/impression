package com.afollestad.impression.accounts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;

import com.afollestad.impression.api.MediaEntry;
import com.afollestad.impression.api.MediaFolderEntry;
import com.afollestad.impression.media.MediaAdapter;
import com.afollestad.inquiry.annotations.Column;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import rx.Single;

public class Account {

    public static final int TYPE_LOCAL = 1;
    public static final int TYPE_PICASA = 2;
    public static final int TYPE_GOOGLE_DRIVE = 3;
    public static final int TYPE_DROPBOX = 4;

    public static final String TABLE = "account";

    @Column(name = "_id", primaryKey = true, autoIncrement = true)
    protected long mId;
    @Column
    protected String mUsername;
    @Column
    @Type
    protected int mType;

    protected AccountHelper mHelper;

    public Account() {
        mId = -1;
        mUsername = null;
        mHelper = null;
    }

    protected Account(Context context, String name, @Type int type) {
        mId = -1;
        mUsername = name;
        mType = type;

        updateHelper();
    }

    public void updateHelper() {
        switch (mType) {
            case TYPE_LOCAL:
                mHelper = new LocalHelper(this);
                break;
            case TYPE_PICASA:
                mHelper = new PicasaHelper(this);
                break;
            case TYPE_GOOGLE_DRIVE:
            case TYPE_DROPBOX:
            default:
                mHelper = null;
                break;
        }
    }

    public final long getId() {
        return mId;
    }

    @Type
    public final int getType() {
        return mType;
    }

    public final String name() {
        return mUsername;
    }

    public Single<? extends Set<? extends MediaFolderEntry>> getMediaFolders(Context context, @MediaAdapter.SortMode int sort, @MediaAdapter.FileFilterMode int filter) {
        return mHelper.getMediaFolders(context, sort, filter);
    }

    public Single<List<MediaEntry>> getEntries(Context context, String albumPath, boolean explorerMode, @MediaAdapter.SortMode int sort, @MediaAdapter.FileFilterMode int filter) {
        return mHelper.getEntries(context, albumPath, explorerMode, sort, filter);
    }

    public Drawable getProfileImage(Context context) {
        return mHelper.getProfileImage(context);
    }

    public Drawable getHeader(Context context) {
        return mHelper.getHeader(context);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Account
                && mId == ((Account) o).mId
                && mType == ((Account) o).mType
                && mUsername.equals(((Account) o).mUsername);
    }

    @Override
    public int hashCode() {
        int i = 37;
        i = i * 37 + (int) (mId ^ (mId >>> 32));
        i = i * 37 + mUsername.hashCode();
        i = i * 37 + mType;

        return i;
    }

    @IntDef({TYPE_LOCAL, TYPE_PICASA, TYPE_GOOGLE_DRIVE, TYPE_DROPBOX})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }
}
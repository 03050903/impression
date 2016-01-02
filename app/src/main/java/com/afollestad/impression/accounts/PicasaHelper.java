package com.afollestad.impression.accounts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.afollestad.impression.api.MediaEntry;
import com.afollestad.impression.api.MediaFolderEntry;
import com.afollestad.impression.media.MediaAdapter;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Single;
import rx.SingleSubscriber;

public class PicasaHelper extends AccountHelper {

    GoogleApiClient mClient;

    String mCoverUrl;

    public PicasaHelper(Account account) {
        super(account);
    }

    public static Account newInstance(Context context, String email) {
        return new Account(context, email, Account.TYPE_PICASA);
    }

    public void connect(final Context context) {
        mClient = new GoogleApiClient.Builder(context)
                .enableAutoManage((FragmentActivity) context, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                })
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {

                        String accountId = null;
                        try {
                            accountId = GoogleAuthUtil.getAccountId(context, mAccount.name());
                        } catch (GoogleAuthException | IOException e) {
                            e.printStackTrace();
                        }
                        Plus.PeopleApi.load(mClient, accountId)
                                .setResultCallback(new ResultCallbacks<People.LoadPeopleResult>() {
                                    @Override
                                    public void onSuccess(@NonNull People.LoadPeopleResult loadPeopleResult) {
                                        String coverUrl = loadPeopleResult.getPersonBuffer().get(0).getCover().getCoverPhoto().getUrl();
                                    }

                                    @Override
                                    public void onFailure(@NonNull Status status) {

                                    }
                                });
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();
    }

    @Override
    public Single<? extends Set<? extends MediaFolderEntry>> getMediaFolders(Context context, @MediaAdapter.SortMode int sortMode, @MediaAdapter.FileFilterMode int filter) {
        return Single.create(new Single.OnSubscribe<Set<? extends MediaFolderEntry>>() {
            @Override
            public void call(SingleSubscriber<? super Set<? extends MediaFolderEntry>> singleSubscriber) {
                singleSubscriber.onSuccess(new HashSet<MediaFolderEntry>());
            }
        });
    }

    @Override
    public Single<List<MediaEntry>> getEntries(Context context, String albumPath, boolean explorerMode, @MediaAdapter.SortMode int sort, @MediaAdapter.FileFilterMode int filter) {
        return Single.create(new Single.OnSubscribe<List<MediaEntry>>() {
            @Override
            public void call(SingleSubscriber<? super List<MediaEntry>> singleSubscriber) {
                singleSubscriber.onSuccess(new ArrayList<MediaEntry>());
            }
        });
    }

    @Override
    public Drawable getHeader(Context context) {
        return null;
    }

    @Override
    public boolean supportsExplorerMode() {
        return false;
    }
}

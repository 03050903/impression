package com.afollestad.impression.accounts;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.afollestad.impression.api.MediaEntry;
import com.afollestad.impression.api.MediaFolderEntry;
import com.afollestad.impression.media.MediaAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;

public class PicasaHelper extends AccountHelper {
    Uri mCoverUrl;
    Uri mProfileUrl;
    private GoogleApiClient mClient;

    public PicasaHelper(Account account) {
        super(account);
    }

    public static Account newInstance(Context context, String email) {
        return new Account(context, email, Account.TYPE_PICASA);
    }

    public Single<Uri[]> getProfileImageUris(final Context context) {
        if (mCoverUrl == null || mProfileUrl == null) {
            return Single.create(new Single.OnSubscribe<Uri[]>() {
                @Override
                public void call(final SingleSubscriber<? super Uri[]> singleSubscriber) {
                    OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mClient);
                    opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                        @Override
                        public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                            singleSubscriber.onError(new Exception(googleSignInResult.getStatus().toString()));

                            Plus.PeopleApi.load(mClient, mAccount.name())
                                    .setResultCallback(new ResultCallbacks<People.LoadPeopleResult>() {
                                        @Override
                                        public void onSuccess(@NonNull People.LoadPeopleResult loadPeopleResult) {
                                            mCoverUrl = Uri.parse(loadPeopleResult.getPersonBuffer().get(0).getCover().getCoverPhoto().getUrl());
                                            mProfileUrl = Uri.parse(loadPeopleResult.getPersonBuffer().get(0).getImage().getUrl());

                                            mClient.disconnect();

                                            singleSubscriber.onSuccess(new Uri[]{mProfileUrl, mCoverUrl});
                                        }

                                        @Override
                                        public void onFailure(@NonNull Status status) {
                                            singleSubscriber.onError(new Exception(status.toString()));
                                        }
                                    });
                        }
                    });

                    // mClient.connect();
                }
            }).subscribeOn(AndroidSchedulers.mainThread());
        } else {
            return Single.just(new Uri[]{mProfileUrl, mCoverUrl});
        }
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
    public boolean supportsExplorerMode() {
        return false;
    }

}

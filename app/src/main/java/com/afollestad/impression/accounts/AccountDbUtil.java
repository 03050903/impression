package com.afollestad.impression.accounts;

import android.content.Context;

import com.afollestad.impression.utils.PrefUtils;
import com.afollestad.inquiry.Inquiry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class AccountDbUtil {

    public static Single<Account> getCurrentAccount(final Context context) {
        final long currentId = PrefUtils.getCurrentAccountId(context);
        return Observable.create(new Observable.OnSubscribe<List<Account>>() {
            @Override
            public void call(Subscriber<? super List<Account>> subscriber) {
                if (currentId == -1) {
                    Account account = LocalHelper.newInstance(context);
                    Inquiry.get()
                            .insertInto(Account.TABLE, Account.class)
                            .values(account)
                            .run();

                    PrefUtils.setCurrentAccountId(context, account.getId());
                }
                subscriber.onCompleted();
            }
        }).switchIfEmpty(getAllAccounts().toObservable())
                .toSingle()
                .map(new Func1<List<Account>, Account>() {
                    @Override
                    public Account call(List<Account> accounts) {
                        Account current = accounts.get(0);
                        for (Account a : accounts) {
                            if (a.getId() == currentId) {
                                current = a;
                                break;
                            }
                        }
                        return current;
                    }
                });
    }

    public static Single<List<Account>> getAllAccounts() {
        return Single.create(new Single.OnSubscribe<List<Account>>() {
            @Override
            public void call(SingleSubscriber<? super List<Account>> singleSubscriber) {
                Account[] all = Inquiry.get()
                        .selectFrom(Account.TABLE, Account.class)
                        .all();

                for (Account account : all) {
                    account.updateHelper();
                }

                singleSubscriber.onSuccess(new ArrayList<>(Arrays.asList(all)));
            }
        }).subscribeOn(Schedulers.io());
    }

    public static void addAccount(Account account) {
        Inquiry.get()
                .insertInto(Account.TABLE, Account.class)
                .values(account)
                .run();
    }
}

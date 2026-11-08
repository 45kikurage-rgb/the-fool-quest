package com.aruno.foolquest.widget.next;

/** Preserve pre-migration preferences before any Activity/receiver/job can write. */
public final class ResultApplication extends android.app.Application {
    @Override public void onCreate(){super.onCreate();BackupCoordinator.get(this);}
}

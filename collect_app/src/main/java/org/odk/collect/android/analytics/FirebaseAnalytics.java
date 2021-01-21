package org.odk.collect.android.analytics;

import android.os.Bundle;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.GeneralSharedPreferences;

public class FirebaseAnalytics implements Analytics {

    private final com.google.firebase.analytics.FirebaseAnalytics firebaseAnalytics;
    private final GeneralSharedPreferences generalSharedPreferences;
    private final FirebaseCrashlytics crashlytics;

    public FirebaseAnalytics(com.google.firebase.analytics.FirebaseAnalytics firebaseAnalytics, GeneralSharedPreferences generalSharedPreferences) {
        this.firebaseAnalytics = firebaseAnalytics;
        this.generalSharedPreferences = generalSharedPreferences;
        setupRemoteAnalytics();

        crashlytics = FirebaseCrashlytics.getInstance();
    }

    @Deprecated
    @Override
    public void logEvent(String category, String action) {
        Bundle bundle = new Bundle();
        bundle.putString("action", action);
        firebaseAnalytics.logEvent(category, bundle);
    }

    @Deprecated
    @Override
    public void logEvent(String category, String action, String label) {
        Bundle bundle = new Bundle();
        bundle.putString("action", action);
        bundle.putString("label", label);
        firebaseAnalytics.logEvent(category, bundle);
    }

    @Override
    public void logFormEvent(String event, String formId) {
        Bundle bundle = new Bundle();
        bundle.putString("form", formId);
        firebaseAnalytics.logEvent(event, bundle);
    }

    @Override
    public void logFatal(Throwable throwable) {
        crashlytics.recordException(throwable);
    }

    @Override
    public void logNonFatal(String message) {
        crashlytics.log(message);
    }

    @Override
    public void logServerEvent(String event, String serverHash) {
        Bundle bundle = new Bundle();
        bundle.putString("server", serverHash);
        firebaseAnalytics.logEvent(event, bundle);
    }

    private void setupRemoteAnalytics() {
        boolean isAnalyticsEnabled = generalSharedPreferences.getBoolean(GeneralKeys.KEY_ANALYTICS, true);
        setAnalyticsCollectionEnabled(isAnalyticsEnabled);
    }

    public void setAnalyticsCollectionEnabled(boolean isAnalyticsEnabled) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(isAnalyticsEnabled);
    }

    @Override
    public void setUserProperty(String name, String value) {
        firebaseAnalytics.setUserProperty(name, value);
    }
}

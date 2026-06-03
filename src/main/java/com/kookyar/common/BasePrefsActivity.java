package com.kookyar.common;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

/**
 * Base preferences activity that hosts a PreferenceFragmentCompat.
 * Subclasses provide the preferences XML resource.
 */
public abstract class BasePrefsActivity extends AppCompatActivity {

    /** Return the layout resource for the preferences activity container. */
    protected abstract int getLayoutResource();

    /** Return the container view ID where the preference fragment is placed. */
    protected abstract int getFragmentContainerId();

    /** Return the preference XML resource (e.g., R.xml.preferences). */
    protected abstract int getPreferencesResource();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(getFragmentContainerId(), createPreferenceFragment())
                    .commit();
        }
    }

    protected PreferenceFragmentCompat createPreferenceFragment() {
        return new InternalPrefsFragment(getPreferencesResource());
    }

    public static class InternalPrefsFragment extends PreferenceFragmentCompat {
        private final int prefsResource;

        public InternalPrefsFragment(int prefsResource) {
            this.prefsResource = prefsResource;
        }

        public InternalPrefsFragment() {
            this.prefsResource = 0;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if (prefsResource != 0) {
                setPreferencesFromResource(prefsResource, rootKey);
            }
        }
    }
}

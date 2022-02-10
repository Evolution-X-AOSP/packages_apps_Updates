package org.evolution.ota.misc;

import android.os.AsyncTask;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import org.evolution.ota.UpdatesActivity;
import org.evolution.ota.UpdatesListAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchChangelog extends AsyncTask {

    private String changelog = "";
    public String customURL;

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            Context applicationContext = UpdatesActivity.getContextExt();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            customURL = prefs.getString(Constants.PREF_CUSTOM_OTA_URL, Constants.OTA_URL);
            URL url = new URL(String.format(customURL + "/changelogs/%s/%s.txt",
                    Utils.getDeviceCodeName(), Constants.fileName));
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while (line != null) {
                line = bufferedReader.readLine();
                if (line != null && !line.trim().equals(""))
                    changelog = changelog + line + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        UpdatesListAdapter.changelogData = this.changelog;
    }
}

package org.evolution.ota.misc;

import android.os.AsyncTask;

import org.evolution.ota.UpdatesListAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchChangelog extends AsyncTask {

    private String changelog = "";

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            URL url = new URL(String.format(Constants.DOWNLOAD_WEBPAGE_URL,
                    Utils.getDeviceCodeName(), Constants.fileName));
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while (line != null) {
                line = bufferedReader.readLine();
                if (line != null && !line.trim().equals(""))
                    changelog = changelog + "- " + line + "\n";
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

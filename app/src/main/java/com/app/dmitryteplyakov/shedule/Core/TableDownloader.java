package com.app.dmitryteplyakov.shedule.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.app.dmitryteplyakov.shedule.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by dmitry21 on 29.10.17.
 */

public class TableDownloader {
    private static final String URLBUIDLER_TAG = "UrlBuilder";
    private static final String DOWNLOADER_TAG = "Downloader";
    private Context mContext;
    private String mFilename;

    public TableDownloader(Context context, String filename) {
        mContext = context;
        mFilename = filename;
    }

    private String urlBuilder() {
        String urlPart = null;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String faculty = sharedPreferences.getString("faculty", "0");
        String spec = sharedPreferences.getString("spec", "0");
        String course = sharedPreferences.getString("course", "0");
        String stream = sharedPreferences.getString("stream", "0");
        String file_url = null;
        String fix = mContext.getString(R.string.m);
        String evsakAsAKFix = mContext.getString(R.string.ak);


        Log.d(URLBUIDLER_TAG, "Check " + file_url);
        if(course.equals("0") || faculty.equals("0") || spec.equals("0") || stream.equals("0")) {
            Log.d(URLBUIDLER_TAG, "Data isn't fully!");
            return null;
        }

        try {
            if (compareRule(faculty, spec)) {
                if(spec.equals(mContext.getString(R.string.evsak))) {
                    urlPart = faculty + "/" + evsakAsAKFix + "/" + spec + " " + course + "-" + stream + ".xls";
                }
                else {
                    urlPart = faculty + "/" + spec.substring(0, spec.length() - 1) + "/" + spec + " " + course + "-" + stream + ".xls";
                }
            }
            else if (faculty.equals(mContext.getString(R.string.mech_link))) {
                urlPart = faculty + "/" + fix + "/" + spec + " " + course + "-" + stream + ".xls";
            }
            else if (spec.equals(mContext.getString(R.string.rst))) {
                urlPart = faculty + "/" + mContext.getString(R.string.rs) + "/" + spec + " " + course + "-" + stream + ".xls";
            }
            else if (spec.equals(mContext.getString(R.string.uvdbobp))) {
                urlPart = faculty + "/" + mContext.getString(R.string.uvd) + "/" + mContext.getString(R.string.uvd) + " " + course + "-" + stream + " " + mContext.getString(R.string.obp) + ".xls";
            }


            file_url = "http://mstuca.ru/students/schedule/" + URLEncoder.encode(urlPart, "UTF-8").replaceAll("\\+", "%20").replaceAll("%2F", "/");
        } catch (UnsupportedEncodingException e) {

        }
        return file_url;
    }

    private boolean compareRule(String faculty, String spec) {
        return (!faculty.equals(mContext.getString(R.string.mech_link)) && !spec.equals(mContext.getString(R.string.rst)) && !spec.equals(mContext.getString(R.string.uvdbobp)));
    }

    private boolean Downloader(int sheet) {
        /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String faculty = sharedPreferences.getString("faculty", "0");
        String spec = sharedPreferences.getString("spec", "0");
        String course = sharedPreferences.getString("course", "0");
        String stream = sharedPreferences.getString("stream", "0");
        String file_url = null;
        String fix = mContext.getString(R.string.m);
        */
        //Log.d("PARSERHTML", pageParser());

        Log.d(DOWNLOADER_TAG, "SHEET: " + Integer.toString(sheet));
        /*if(sheet != 0 && isNotGlobalChanges) {
            Log.d("sheduleDownloader", "Is not global changes in shedule. Skip downloading...");
            return true;
        }*/

        /*if (course.equals("0")) {
            turnOff = true;
            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.select_started_hint_snackbar), Snackbar.LENGTH_LONG).show();
            return false;
        }*/
        //Log.d("ERERRE", spec);
        /*String urlPart = null;
        try {
            if(!faculty.equals(mContext.getString(R.string.mech_link)) && !spec.equals(mContext.getString(R.string.rst)) && !spec.equals(mContext.getString(R.string.uvdbobp)))
                urlPart = faculty + "/" + spec.substring(0, spec.length() - 1) + "/" + spec + " " + course + "-" + stream + ".xls";
            else if(faculty.equals(mContext.getString(R.string.mech_link)))
                urlPart = faculty + "/" + fix + "/" + spec + " " + course + "-" + stream + ".xls";
            else if(spec.equals(mContext.getString(R.string.rst)))
                urlPart = faculty + "/" + mContext.getString(R.string.rs) + "/" + spec + " " + course + "-" + stream + ".xls";
            else if(spec.equals(mContext.getString(R.string.uvdbobp)))
                urlPart = faculty + "/" + mContext.getString(R.string.uvd) + "/" + mContext.getString(R.string.uvd) + " " + course + "-" + stream + " " + mContext.getString(R.string.obp) +".xls";


            file_url = "http://mstuca.ru/students/schedule/" + URLEncoder.encode(urlPart, "UTF-8").replaceAll("\\+", "%20").replaceAll("%2F", "/");
        } catch(UnsupportedEncodingException e) {

        }*/
        String file_url = urlBuilder();
        Log.d(DOWNLOADER_TAG, "Check " + file_url);
        if(file_url == null) {
            return false;
        }

        InputStream input = null;
        FileOutputStream output = null;
        FileInputStream localeShedule = null;
        try {
            URL url = new URL(file_url);
            URLConnection connection = url.openConnection();
            Log.d("URL", connection.getURL().toString());
            input = connection.getInputStream();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            input = connection.getInputStream();
            Log.d("sheduleDownloader", "Type: " + con.getContentType() + " RESP: " + con.getResponseCode() + " RESPMSG: " + con.getResponseMessage());


            String lastModRemoteString = connection.getHeaderField("Last-Modified");
            SimpleDateFormat dateformatter = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            Date lastModRemote = new Date(connection.getLastModified());
            File localFile = new File(mContext.getFilesDir(), mFilename);
            Log.d("Already?", Boolean.toString(localFile.exists()));
            ///////////////////// Rework need
            /*Log.d("Course changed?", Boolean.toString(isCourseChanged));


            if (isDbDrop && localFile.exists() && !isCourseChanged)
                return true;

            if (!forcedUpdate && !isCourseChanged) {
                Log.d("SLFDownloader", "Forced updating disabled");
                if (localFile.exists()) {
                    localeShedule = mContext.openFileInput(filename);
                    Date lastModLocal = new Date(localFile.lastModified());
                    Log.d("SLFDownloader", lastModLocal.toString() + " " + lastModRemote.toString());
                    if ((lastModLocal.equals(lastModRemote) || lastModLocal.after(lastModRemote))) {
                        if (swipeRefresh)
                            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.data_already_fresh), Snackbar.LENGTH_SHORT).show();
                        Log.d("SLFDownloader", "Data is fresh. Skip downloading...");
                        turnOff = true;
                        return false;
                    }
                }
            }

            forcedUpdate = false;
            */



            output = mContext.openFileOutput(mFilename, Context.MODE_PRIVATE);

            int read;
            byte[] data = new byte[1024];
            while ((read = input.read(data)) != -1) output.write(data, 0, read);
            output.flush();


            /*if (swipeRefresh)
                Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.data_updated), Snackbar.LENGTH_SHORT).show();
            return true;
*/
        } catch (UnknownHostException e) {
            Log.e("SLFDownloader", "Wrong address or cannot connecting to internet?", e);
            //turnOff = true;
            return false;
        } catch(FileNotFoundException e) {
            Snackbar.make(((AppCompatActivity) mContext).findViewById(R.id.snackbar_layout), mContext.getString(R.string.filenotfound_snackbar), Snackbar.LENGTH_LONG).show();
            //turnOff = true;
            return false;
        } catch(IOException e) {
            Log.e("SheduleDownloader", "Error IO " + e);
            //turnOff = true;
            return false;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {
            }
        }
        return true;
    }

}

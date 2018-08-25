package com.app.dmitryteplyakov.shedule;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.app.dmitryteplyakov.shedule.Core.Discipline;
import com.app.dmitryteplyakov.shedule.Core.DisciplineStorage;
import com.app.dmitryteplyakov.shedule.Core.TableParser;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class SheduleListFragment extends Fragment {

    private Discipline mDiscipline;
    private RecyclerView mRecyclerView;
    private SheduleAdapter mAdapter;

    private LinearLayoutManager linearLayoutManager;
    private RecyclerView.SmoothScroller smoothScroller;
    boolean dSuccess;
    private String EXTRA_SAVED;
    private SwipeRefreshLayout mSwipeRefreshData;
    private boolean swipeRefresh;
    private boolean forcedUpdate;
    private boolean notFirstRun;
    private String filename = "shedule.xls";
    private LinearLayoutManager gridLayoutManager;
    private boolean onceDiscipline;
    private boolean isSubgroup;
    private boolean isDbDrop;
    private boolean turnOff;
    private volatile boolean inProcess;
    private static boolean isCourseChanged;
    private static final int REQUEST_DATE = 5;
    private static final String DIALOG_DATE = "com.app.shedulelistfragment.dialog_date";
    private static boolean isNotGlobalChanges;
    private static boolean resetPosition;
    private DividerItemDecoration mDividerItemDecorator;
    private AsyncLoader loader;
    private AsyncUpdater updater;
    private Handler mHandler;
    private static boolean needUpdateAfterSettings;
    private ClipboardManager mClipboardManager;

    public static void setNeedUpdate(boolean need) {
        needUpdateAfterSettings = need;
    }

    public static void setResetPosition(boolean resetPositionArg) {
        resetPosition = resetPositionArg;
    }

    public static void setIsNotGlobalChanges(boolean isNotGlobalChanges) {
        SheduleListFragment.isNotGlobalChanges = isNotGlobalChanges;
    }

    public static void setIsCourseChanged(boolean state) {
        isCourseChanged = state;
    }

    public void dbDropped() {
        isDbDrop = true;
    }
    public void dropDb() {
        DisciplineStorage.get(getActivity()).resetDb();
        isDbDrop = true;
    }

    /*public String pageParser() {
        String links = null;
        try {
            Document doc = Jsoup.connect("http://mstuca.ru/students/schedule/").get();
            Elements metaElements = doc.select("a.section-title");
            links = metaElements.html();
            return links;
        } catch(IOException e) {

        }
        return links;
    }*/


    private boolean downloadFile(Context mContext, int sheet, boolean first) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String faculty = sharedPreferences.getString("faculty", "0");
        String spec = sharedPreferences.getString("spec", "0");
        String course = sharedPreferences.getString("course", "0");
        String stream = sharedPreferences.getString("stream", "0");
        String file_url = null;
        String fix = getString(R.string.m);
        String specsymb = "-";
        //Log.d("PARSERHTML", pageParser());

        Log.d("sheduleDownloader", "SHEET: " + Integer.toString(sheet));
        if(sheet != 0 && isNotGlobalChanges) {
            Log.d("sheduleDownloader", "Is not global changes in shedule. Skip downloading...");
            return true;
        }

        if (course.equals("0")) {
            turnOff = true;
            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.select_started_hint_snackbar), Snackbar.LENGTH_LONG).show();
            return false;
        }
        Log.d("ERERRE", spec);
        String urlPart = null;
        try {
            if(!faculty.equals(getString(R.string.mech_link)) && !spec.equals(getString(R.string.rst)) && !spec.equals(getString(R.string.uvdbobp))) {
                // Shitburger from MSTUCA
                /*if(spec.equals(getString(R.string.app_math_val)) && course.equals("1")) {
                    stream = "";
                    specsymb = "";
                }*/
                urlPart = faculty + "/" + spec.substring(0, spec.length() - 1) + "/" + spec + " " + course + specsymb + stream + ".xls";
            }
            else if(faculty.equals(getString(R.string.mech_link)))
                urlPart = faculty + "/" + fix + "/" + spec + " " + course + "-" + stream + ".xls";
            else if(spec.equals(getString(R.string.rst)))
                urlPart = faculty + "/" + getString(R.string.rs) + "/" + spec + " " + course + "-" + stream + ".xls";
            else if(spec.equals(getString(R.string.uvdbobp)))
                urlPart = faculty + "/" + getString(R.string.uvd) + "/" + getString(R.string.uvd) + " " + course + "-" + stream + " " + getString(R.string.obp) +".xls";


            file_url = "http://mstuca.ru/students/schedule/" + URLEncoder.encode(urlPart, "UTF-8").replaceAll("\\+", "%20").replaceAll("%2F", "/");
        } catch(UnsupportedEncodingException e) {

        }
        Log.d("SLFDownloader", "Check " + file_url);
        if(course.equals("0") || faculty.equals("0") || spec.equals("0") || stream.equals("0")) {
            Log.d("SLFDownloader", "Data isn't fully!");
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
            File localFile = new File(mContext.getFilesDir(), filename);
            Log.d("Already?", Boolean.toString(localFile.exists()));
            Log.d("Course changed?", Boolean.toString(isCourseChanged));


            if (isDbDrop && localFile.exists() && !isCourseChanged)
                return true;

            if (!forcedUpdate && !isCourseChanged) {
                Log.d("SLFDownloader", "Forced updating disabled");
                if (localFile.exists()) {
                    localeShedule = mContext.openFileInput(filename);
                    Date lastModLocal = new Date(localFile.lastModified());
                    Log.d("SLFDownloader", lastModLocal.toString() + " " + lastModRemote.toString());
                    if ((lastModLocal.equals(lastModRemote) || lastModLocal.after(lastModRemote))) {
                        if (swipeRefresh && first)
                            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.data_already_fresh), Snackbar.LENGTH_SHORT).show();
                        Log.d("SLFDownloader", "Data is fresh. Skip downloading...");
                        turnOff = true;
                        return false;
                    }
                }
            }

            forcedUpdate = false;
            dropDb();
            swipeRefresh = true;



            output = mContext.openFileOutput(filename, Context.MODE_PRIVATE);

            int read;
            byte[] data = new byte[1024];
            while ((read = input.read(data)) != -1) output.write(data, 0, read);
            output.flush();


            if (swipeRefresh && first) {
                Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.data_updated), Snackbar.LENGTH_SHORT).show();
            }
            swipeRefresh = false;
            return true;

        } catch (UnknownHostException e) {
            Log.e("SLFDownloader", "Wrong address or cannot connecting to internet?", e);
            turnOff = true;
            return false;
        } catch(FileNotFoundException e) {
            if(first)
                Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.filenotfound_snackbar), Snackbar.LENGTH_LONG).show();
            turnOff = true;
            return false;
        } catch(IOException e) {
            Log.e("SheduleDownloader", "Error IO " + e);
            turnOff = true;
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

    }

    @Override
    public void onResume() {
        super.onResume();
        if (DisciplineStorage.get(getActivity()).getDisciplines().size() == 0 && needUpdateAfterSettings) {
            Log.d("SLF", "Rebase after DB RESET ");
            //isDbDrop = true;
            if(!isCourseChanged) {
                setIsNotGlobalChanges(true);
            }
            dropDb();
            AsyncLoader loader = new AsyncLoader(0);
            loader.execute(getActivity());
            Log.d("ONRESUME", "GLOBAL! " + Boolean.toString(isCourseChanged));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if(savedInstanceState.getSerializable(EXTRA_SAVED) != null)
                notFirstRun = (boolean) savedInstanceState.getSerializable(EXTRA_SAVED);
        }
        setRetainInstance(true);
    }

    public boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        } catch(NullPointerException ex) {
            Log.e("NetworkChecker", "ERR: ", ex);
            return false;
        }
    }

    private void workingOn(final Context mContext, final int sheet, final boolean first) {
        dSuccess = false;
        Log.d("workingOn", Integer.toString(sheet));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(isNotGlobalChanges) {
                    dSuccess = isNotGlobalChanges;
                    return;
                }
                Log.d("DownloadTask", "Download started!");
                dSuccess = downloadFile(mContext, sheet, first);
            }
        });
        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //sheduleReader(dSuccess, 0, 0, 0);
                //isSubgroup = true;
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                int langGroup;
                int labGroup;
                if (Integer.parseInt(sharedPreferences.getString("subgroup_lang", getString(R.string.first))) == 1)
                    langGroup = 2;
                else
                    langGroup = 3;
                if (Integer.parseInt(sharedPreferences.getString("subgroup_lab", getString(R.string.first))) == 1)
                    labGroup = 2;
                else
                    labGroup = 3;
                new TableParser(dSuccess, sheet, labGroup, langGroup, filename, mContext).parse();
            }
        });

        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {

        }
        readThread.start();
        try {
            readThread.join();
        } catch (InterruptedException e) {

        }

        if (dSuccess)
            Log.d("DownloadTask", "Download finished!");
        else
            Log.d("DownloadTask", "Download cancelled!");
        swipeRefresh = false;
    }

    private void checkStarter(Context mContext, int sheet, boolean first) {
        Thread thread = null;

        if (isOnline(mContext) || isNotGlobalChanges) {
            workingOn(mContext, sheet, first);
        } else {
            if(first)
                Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.error_connection), Snackbar.LENGTH_LONG).show();
            turnOff = true;
        }
        Log.d("SLF", "Connection state: " + Boolean.toString(isOnline(mContext)));
    }

    private class AsyncUpdater extends AsyncTask<Context, Void, Void> {
        private Context localContext;

        @Override
        protected void onPreExecute() {
            mSwipeRefreshData.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(Context... contexts) {
            //for (Context context : contexts)
            localContext = contexts[0];
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            updateUI(localContext);
            mSwipeRefreshData.setRefreshing(false);
            super.onPostExecute(result);
        }
    }

    private class AsyncLoader extends AsyncTask<Context, Void, Void> {
        private Context localContext;
        private int sheet;

        public AsyncLoader(int sheet) {
            this.sheet = sheet;
        }

        @Override
        protected void onPreExecute() {
            mSwipeRefreshData.setRefreshing(true);
            turnOff = false;
            inProcess = true;
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Context... contexts) {
            for (Context context : contexts)
                localContext = context;
            //DisciplineStorage.get(localContext).resetDb();
            Log.d("AsyncLoader PRE", "Size: " + Integer.toString(DisciplineStorage.get(localContext).getDisciplines().size()));
            //for(int i = 0; i < 4; i++) {
            //    if(i == 1)
            //        continue;
            //    forcedUpdate = true;
            //    Log.d("Thread CHECKSTARTER", "SHEET INDEX: " + Integer.toString(i));
            //    checkStarter(localContext, i);
            if(DisciplineStorage.get(localContext).getDisciplines().size() == 0)
                forcedUpdate = true;
            Log.d("AsyncLoader", "Forced update: " + Boolean.toString(forcedUpdate));
            checkStarter(localContext, 0, true);
            if(turnOff)
                return null;
            setIsNotGlobalChanges(true);
            checkStarter(localContext, 2, false);
            checkStarter(localContext, 3, false);
            //}
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("AsyncLoader", "Thread closed.");
            updateUI(localContext);
            mSwipeRefreshData.setRefreshing(false);
            if (isDbDrop)
                isDbDrop = false;
            setIsCourseChanged(false);
            if(resetPosition) {
                Log.d("AsyncLoader", "Thread closed.");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int pos = DisciplineStorage.get(getActivity()).countDisciplinesToDate(new Date(), null);
                        smoothScroller.setTargetPosition(pos);
                        //linearLayoutManager.startSmoothScroll(smoothScroller);
                        //gridLayoutManager.startSmoothScroll(smoothScroller);
                        linearLayoutManager.scrollToPositionWithOffset(pos, 0);
                    }
                });
            }
            setResetPosition(false);
            setIsNotGlobalChanges(false);
            inProcess = false;
            Log.d("AsyncLoader AFTER", Integer.toString(DisciplineStorage.get(localContext).getDisciplines().size()));
            super.onPostExecute(result);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shedule_list, container, false);
        setHasOptionsMenu(true);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.shedule_list_recycler_view);
        mSwipeRefreshData = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh);
        mSwipeRefreshData.setColorSchemeResources(R.color.colorPrimary);
        smoothScroller = new LinearSmoothScroller(getActivity()) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mSwipeRefreshData.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefresh = true;
                AsyncLoader loader = new AsyncLoader(1);
                loader.execute(getActivity());

            }
        });
        mDividerItemDecorator = new DividerItemDecoration(mRecyclerView.getContext(), linearLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(mDividerItemDecorator);

        mClipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        updateUI(getActivity());
        if(sharedPreferences.getBoolean("check_update_when_start", true)) {
            AsyncLoader loader = new AsyncLoader(0);
            loader.execute(getActivity());
        } else {
            AsyncUpdater updater = new AsyncUpdater();
            updater.execute(getActivity());
        }


        getActivity().findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int pos = DisciplineStorage.get(getActivity()).countDisciplinesToDate(new Date(), null);
                        smoothScroller.setTargetPosition(pos);
                        //linearLayoutManager.startSmoothScroll(smoothScroller);
                        //gridLayoutManager.startSmoothScroll(smoothScroller);
                        linearLayoutManager.scrollToPositionWithOffset(pos, 0);
                    }
                });
            }
        });
        //updateUI(getActivity());

        return v;
    }

    // Реализация адаптера
    private class SheduleHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener { // Реализация Holder. Также реализует интерфейс OnClickListener ля обработки нажатий на View
        public TextView mTeacherNameTextView; // Объекты макета list_item
        public TextView mDiscipleNameTextView;
        public TextView mAuditoryTextView;
        public TextView mLectureTypeTextView;
        public TextView mDate;
        private TextView mDurationTime;
        private Discipline mDiscipline;
        private TextView mPairNumber;

        public SheduleHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            mTeacherNameTextView = (TextView) itemView.findViewById(R.id.teacherName); // Связывание объектов макета list_item с переменными
            mDiscipleNameTextView = (TextView) itemView.findViewById(R.id.disciple_name);
            mAuditoryTextView = (TextView) itemView.findViewById(R.id.auditory);
            mLectureTypeTextView = (TextView) itemView.findViewById(R.id.lecture_type);
            mDurationTime = (TextView) itemView.findViewById(R.id.start_time);
            mDate = (TextView) itemView.findViewById(R.id.date);
            mPairNumber = (TextView) itemView.findViewById(R.id.pair_counter);

        }

        public void bindShedule(Discipline discipline) {
            mDiscipline = discipline;
            mTeacherNameTextView.setText(discipline.getTeacherName());
            mDiscipleNameTextView.setText(discipline.getDiscipleName());
            mAuditoryTextView.setText(discipline.getAuditoryNumber());
            mLectureTypeTextView.setText(discipline.getType());
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            mDate.setText(dateFormatter.format(discipline.getDate()));
            mDurationTime.setText(timeFormatter.format(mDiscipline.getDate()) + " - " + timeFormatter.format(new Date(mDiscipline.getEndTime())));
            mPairNumber.setText(Integer.toString(mDiscipline.getNumber()));
        }

        @Override
        public void onClick(View v) {
            //
        }

        @Override
        public boolean onLongClick(View view) {
            String clipboardContent = mDiscipline.getDiscipleName() + " " + mDiscipline.getNumber() + getString(R.string.pair_clipboard) + " " + mDiscipline.getAuditoryNumber();
            ClipData mClipData = ClipData.newPlainText(getString(R.string.app_name), clipboardContent);
            mClipboardManager.setPrimaryClip(mClipData);
            Toast.makeText(getActivity(), getString(R.string.toast_pair_clipboard), Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private class SheduleHolderDate extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener { // Реализация Holder. Также реализует интерфейс OnClickListener ля обработки нажатий на View
        public TextView mTeacherNameTextView; // Объекты макета list_item
        public TextView mDiscipleNameTextView;
        public TextView mAuditoryTextView;
        public TextView mLectureTypeTextView;
        public TextView mDate;
        private TextView mDurationTime;
        private Discipline mDiscipline;
        private TextView mGlobalDate;
        private TextView mPairNumber;

        public SheduleHolderDate(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            mTeacherNameTextView = (TextView) itemView.findViewById(R.id.teacherName); // Связывание объектов макета list_item с переменными
            mDiscipleNameTextView = (TextView) itemView.findViewById(R.id.disciple_name);
            mAuditoryTextView = (TextView) itemView.findViewById(R.id.auditory);
            mLectureTypeTextView = (TextView) itemView.findViewById(R.id.lecture_type);
            mDurationTime = (TextView) itemView.findViewById(R.id.start_time);
            mDate = (TextView) itemView.findViewById(R.id.date);
            mGlobalDate = (TextView) itemView.findViewById(R.id.global_date);
            mPairNumber = (TextView) itemView.findViewById(R.id.pair_counter);
        }

        public void bindShedule(Discipline discipline) {
            mDiscipline = discipline;
            mTeacherNameTextView.setText(discipline.getTeacherName());
            mDiscipleNameTextView.setText(discipline.getDiscipleName());
            mAuditoryTextView.setText(discipline.getAuditoryNumber());
            mLectureTypeTextView.setText(discipline.getType());
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM");
            SimpleDateFormat dateTitleFormatter = new SimpleDateFormat("EEEE, dd MMMM yyyy");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            String dayTitle = dateTitleFormatter.format(mDiscipline.getDate());
            dayTitle = dayTitle.substring(0, 1).toUpperCase() + dayTitle.substring(1);
            mDurationTime.setText(timeFormatter.format(mDiscipline.getDate()) + " - " + timeFormatter.format(new Date(mDiscipline.getEndTime())));

            mPairNumber.setText(Integer.toString(mDiscipline.getNumber()));
            mDate.setText(dateFormatter.format(discipline.getDate()));
            Calendar calCur = Calendar.getInstance();
            calCur.setTime(new Date());
            Calendar calDay = Calendar.getInstance();
            calDay.setTime(discipline.getDate());
            boolean monthEq = calCur.get(Calendar.MONTH) == calDay.get(Calendar.MONTH);
            boolean dayEq = calCur.get(Calendar.DAY_OF_MONTH) == calDay.get(Calendar.DAY_OF_MONTH);
            String headerTitle = null;
            if(monthEq && dayEq) {
                headerTitle = dayTitle + " " + getString(R.string.today_label);
                mGlobalDate.setTypeface(mGlobalDate.getTypeface(), Typeface.BOLD);
            } else {
                headerTitle = dayTitle;
                mGlobalDate.setTypeface(Typeface.DEFAULT);
            }
            mGlobalDate.setText(headerTitle);

        }

        @Override
        public void onClick(View v) {
            //
        }

        @Override
        public boolean onLongClick(View view) {
            String clipboardContent = mDiscipline.getDiscipleName() + " " + mDiscipline.getNumber() + getString(R.string.pair_clipboard) + " " + mDiscipline.getAuditoryNumber();
            ClipData mClipData = ClipData.newPlainText(getString(R.string.app_name), clipboardContent);
            mClipboardManager.setPrimaryClip(mClipData);
            Toast.makeText(getActivity(), getString(R.string.toast_pair_clipboard), Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private class SheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> { // Реализация адаптера
        private List<Discipline> mDisciplines;

        public SheduleAdapter(List<Discipline> disciplines) {
            mDisciplines = disciplines;
        }

        public void setDisciplines(List<Discipline> disciplines) {
            mDisciplines = disciplines;
        }

        @Override
        public int getItemViewType(int position) {
            if (position != 0) {
                Calendar curr = Calendar.getInstance(new Locale("ru"));
                curr.setTime(mDisciplines.get(position - 1).getDate());
                curr.set(Calendar.HOUR_OF_DAY, 0);
                curr.set(Calendar.MINUTE, 0);
                curr.set(Calendar.SECOND, 0);
                curr.set(Calendar.MILLISECOND, 0);

                Calendar after = Calendar.getInstance(new Locale("ru"));
                after.setTime(mDisciplines.get(position).getDate());
                after.set(Calendar.HOUR_OF_DAY, 0);
                after.set(Calendar.MINUTE, 0);
                after.set(Calendar.SECOND, 0);
                after.set(Calendar.MILLISECOND, 0);
                if (!curr.getTime().equals(after.getTime())) {
                    if (curr.getTime().after(after.getTime()))
                        return 1;
                    else
                        return 2;
                } else
                    return 0;
            }
            return 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) { // Создание представления
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = null;
            switch (viewType) {
                case 2:
                    view = layoutInflater.inflate(R.layout.shedule_list_item_date, parent, false);
                    return new SheduleHolderDate(view);
                case 1:
                    view = layoutInflater.inflate(R.layout.shedule_list_item_date, parent, false);
                    return new SheduleHolderDate(view);
                case 0:
                    view = layoutInflater.inflate(R.layout.shedule_list_item, parent, false);
                    return new SheduleHolder(view);
            }
            return null;

        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) { // Замена содержимого View элементами следующих дисциплин
            Discipline discipline = mDisciplines.get(position);
            switch (holder.getItemViewType()) {
                case 2:
                    ((SheduleHolderDate) holder).bindShedule(mDisciplines.get(position));
                    break;
                case 1:
                    ((SheduleHolderDate) holder).bindShedule(discipline);
                    break;
                case 0:
                    ((SheduleHolder) holder).bindShedule(discipline);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mDisciplines.size();
        }
    }

    private synchronized void updateUI(Context mContext) { // Обновление интерфейса. Связывание данных с адаптером
        //List<Discipline> disciplines = DisciplineStorage.get(getActivity()).getDisciplines();
        Calendar calendar = Calendar.getInstance(new Locale("ru"));
        calendar.setTime(new Date());
        //calendar.set(Calendar.MONTH, 8);
        //calendar.set(Calendar.DAY_OF_MONTH, 2);
        List<Discipline> disciplines = DisciplineStorage.get(mContext).getDisciplines();
        Collections.sort(disciplines, Discipline.dateComparator);
        Log.d("DEBUG", Integer.toString(disciplines.size()));
        if (mRecyclerView.getAdapter() == null || mAdapter == null) {
            mAdapter = new SheduleAdapter(disciplines); // Связывание списка данных с адаптером
            //mAdapter.setHasStableIds(true);
            if(isAdded())
                mRecyclerView.setAdapter(mAdapter); // Назначение адаптера к RecyclerView
        } else {
            mAdapter.setDisciplines(disciplines);
            mAdapter.notifyDataSetChanged();
        }
        if (!notFirstRun) {
            notFirstRun = true;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int pos = DisciplineStorage.get(getActivity()).countDisciplinesToDate(new Date(), null);
                    //smoothScroller.setTargetPosition(pos);
                    //linearLayoutManager.startSmoothScroll(smoothScroller);
                    linearLayoutManager.scrollToPositionWithOffset(pos, 0);

                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_toolbar:
                swipeRefresh = true;
                //checkStarter(getActivity());
                AsyncLoader loader = new AsyncLoader(0);
                loader.execute(getActivity());
                return true;
            case R.id.settings:
                Intent intent = PrefActivity.newIntent(getActivity(), "general");
                startActivity(intent);
                return true;
            case R.id.calendar_picker:
                if((DisciplineStorage.get(getActivity()).getDisciplines().size() != 0) && !inProcess) {
                    FragmentManager manager = getFragmentManager();
                    UUID id = DisciplineStorage.get(getActivity()).getDiscipleByNumber(1).getId();
                    if (DisciplineStorage.get(getActivity()).getDiscipleByDate(new Date()) != null)
                        id = DisciplineStorage.get(getActivity()).getDiscipleByDate(new Date()).getId();
                    CalendarDialog dialog = CalendarDialog.newInstance(id);
                    Log.d("return", DisciplineStorage.get(getActivity()).getDisciple(id).getDiscipleName());
                    dialog.setTargetFragment(SheduleListFragment.this, REQUEST_DATE);
                    dialog.show(manager, DIALOG_DATE);
                }
                return true;
        }

        return onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == REQUEST_DATE) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int pos = DisciplineStorage.get(getActivity()).countDisciplinesToDate((Date) data.getSerializableExtra(CalendarDialog.RETURN_DATE), null);
                        Log.d("RES", Integer.toString(pos));
                        //smoothScroller.setTargetPosition(pos);
                        //linearLayoutManager.startSmoothScroll(smoothScroller);
                        linearLayoutManager.scrollToPositionWithOffset(pos, 0);

                    }
                });
            }
        }
    }

}

package com.app.dmitryteplyakov.shedule;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class SheduleListFragment extends Fragment {

    private Disciple mDisciple;
    private RecyclerView mRecyclerView;
    private SheduleAdapter mAdapter;
    private static String file_url = "http://mstuca.ru/students/schedule/webdav_bizproc_history_get/34612/34612/?force_download=1";
    private static String filename = "shedule.xls";

    private void downloadFile() {
        InputStream input = null;
        FileOutputStream output = null;
        try {
            URL url = new URL(file_url);
            input = url.openConnection().getInputStream();
            output = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);

            int read;
            byte[] data = new byte[1024];
            while((read = input.read(data)) != -1)  output.write(data, 0, read);
            output.flush();
        } catch(IOException e) {
            Log.e("SheduleDownloader", "Error IO " + e);
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch(IOException e) {}
        }
    }

    private void sheduleReader() {
        DiscipleStorage mDisciples = DiscipleStorage.get(getActivity());
        String str;
        List<String> parsedStrList = null;
        try {
            HSSFWorkbook myShedule = new HSSFWorkbook(((getActivity().openFileInput(filename))));
            HSSFSheet mySheduleSheet = myShedule.getSheetAt(1);
            HSSFRow row = mySheduleSheet.getRow(1);
            Disciple disciple = new Disciple();
            /*for(int i = 1; i < 195; i++) {
                for(int j = 3; j < 9; j++) {
                    str = mySheduleSheet.getRow(i).getCell(j).getStringCellValue();
                    parsedStrList = Arrays.asList(str.split(","));
                }
                if(parsedStrList.size() == 1) {
                    disciple.setDiscipleName("Занятия нет!");
                } else {
                    disciple.setLectureType(parsedStrList.get(0));
                    disciple.setTeacherName(parsedStrList.get(1) + parsedStrList.get(2));
                    disciple.setAuditoryNumber(parsedStrList.get(3));
                }
                mDisciples.addDisciple(disciple);

            }*/
            str = mySheduleSheet.getRow(33).getCell(3).getStringCellValue();
            disciple.setTeacherName(str);
            mDisciples.addDisciple(disciple);
        } catch(IOException e) {
            Log.e("sheduleReader", "Error read shedule file!");
        }
    }

    class DownloadTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("DownloadTask", "Download started!");
        }

        @Override
        protected Void doInBackground(Void... params) {
                downloadFile();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.d("DownloadTask", "Download finished!");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DownloadTask dt = new DownloadTask();
        dt.execute();
        sheduleReader();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shedule_list, container, false);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.shedule_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        updateUI();
        return v;
    }

    // Реализация адаптера
    private class SheduleHolder extends RecyclerView.ViewHolder implements View.OnClickListener { // Реализация Holder. Также реализует интерфейс OnClickListener ля обработки нажатий на View
        public TextView mTeacherNameTextView; // Объекты макета list_item
        public TextView mDiscipleNameTextView;
        public TextView mAuditoryTextView;
        public TextView mLectureTypeTextView;

        public SheduleHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTeacherNameTextView = (TextView) itemView.findViewById(R.id.teacherName); // Связывание объектов макета list_item с переменными
            mDiscipleNameTextView = (TextView) itemView.findViewById(R.id.disciple_name);
            mAuditoryTextView = (TextView) itemView.findViewById(R.id.auditory);
            mLectureTypeTextView = (TextView) itemView.findViewById(R.id.lecture_type);
        }

        @Override
        public void onClick(View v) {
            //
        }
    }

    private class SheduleAdapter extends RecyclerView.Adapter<SheduleHolder> { // Реализация адаптера
        private List<Disciple> mDisciples;

        public SheduleAdapter(List<Disciple> disciples) {
            mDisciples = disciples;
        }

        @Override
        public SheduleHolder onCreateViewHolder(ViewGroup parent, int viewType) { // Создание представления
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.shedule_list_item, parent, false);
            return new SheduleHolder(view);
        }

        @Override
        public void onBindViewHolder(SheduleHolder holder, int position) { // Замена содержимого View элементами следующих дисциплин
            Disciple disciple = mDisciples.get(position);
            holder.mTeacherNameTextView.setText(disciple.getTeacherName());
            holder.mDiscipleNameTextView.setText(disciple.getDiscipleName());
            holder.mAuditoryTextView.setText(disciple.getAuditoryNumber());
            holder.mLectureTypeTextView.setText(disciple.getLectureType());
        }

        @Override
        public int getItemCount() {
            return mDisciples.size();
        }
    }

    private void updateUI() { // Обновление интерфейса. Связывание данных с адаптером
        DiscipleStorage discipleStorage = DiscipleStorage.get(getActivity());
        List<Disciple> disciples = discipleStorage.getDisciples();
        mAdapter = new SheduleAdapter(disciples); // Связывание списка данных с адаптером
        mRecyclerView.setAdapter(mAdapter); // Назначение адаптера к RecyclerView
    }

}

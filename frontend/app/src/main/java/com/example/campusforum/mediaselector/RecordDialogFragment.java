package com.example.campusforum.mediaselector;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;

import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;

import com.example.campusforum.R;
import com.example.campusforum.databinding.FragmentRecordDialogBinding;
import com.google.common.io.FileWriteMode;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RecordDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecordDialogFragment extends DialogFragment {
    private static final String TAG = "RecordDialogFragment";
    FragmentRecordDialogBinding binding;
    Chronometer chronometer;
    boolean recording =true;
    private String mFileName = null;
    private String mFilePath = null;
    Uri fileUri;
    FileDescriptor fd;
    ParcelFileDescriptor pfd;
    private MediaRecorder mRecorder = null;
    ActivityResultLauncher<Intent>activityResultLauncher;
    private long mStartingTimeMillis = 0;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RecordDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RecordDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RecordDialogFragment newInstance() {
        RecordDialogFragment fragment = new RecordDialogFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding=FragmentRecordDialogBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.startRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onRecord(recording);
                recording =!recording;
            }
        });
        chronometer= binding.chronometer;
        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode()== Activity.RESULT_OK&&result.getData()!=null){
                            fileUri=result.getData().getData();
                            //mFilePath=uri.getPath()
                            try {
                                pfd=getActivity().getContentResolver().openFileDescriptor(fileUri, "wr");
                                fd= pfd.getFileDescriptor();

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG, "onActivityResult: uri:"+fileUri+" Path"+getPath(fileUri));
                            //开始录音

                            try {
                                startRecording();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        super.onViewCreated(view, savedInstanceState);
    }

    private void onRecord(boolean start){
        //Intent intent=new Intent(getActivity(),RecordingService.class);
        if(start){
            //录制 TODO 改变按钮

            Log.d(TAG, "onRecord: Start");
            if(ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.RECORD_AUDIO)
            !=PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onRecord: 无权限！");

            }else{
                setFileNameAndPath();
            }

        }else{
            //TODO 改变图标yh
            chronometer.stop();
            stopRecording();
            Log.d(TAG, "onRecord: Stop");
        }
    }

    public void startRecording() throws FileNotFoundException {

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        //OutputStream fileOutputStream=getActivity().getContentResolver().openOutputStream(fileUri);
        File f=new File(mFilePath);
        if(!f.exists()){
            Log.d(TAG, "startRecording: file does not exists"+f.getAbsolutePath());
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        mRecorder.setOutputFile(fd);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);
        mRecorder.setAudioSamplingRate(44100);
        mRecorder.setAudioEncodingBitRate(192000);

        try {
            mRecorder.prepare();
            mRecorder.start();
            mStartingTimeMillis = System.currentTimeMillis();
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            binding.startRecordBtn.setImageResource(R.drawable.ic_stop_48px);
            binding.startRecordBtn.setBackgroundTintList(new ColorStateList());
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed"+e);
        }
    }
    private String getPath(Uri uri){
        Log.d(TAG, "getPath: "+uri.getEncodedPath());
        Cursor cursor=getActivity().getContentResolver().query(uri,null,null,null);


        int column_index = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    public void setFileNameAndPath() {
            mFileName = getString(R.string.default_sound_name)
                    + "_" + (System.currentTimeMillis()) + ".wav";
            mFilePath = Environment.getExternalStorageDirectory()+"/Music/"+mFileName;
            //mFilePath=Environment.getExternalStorageDirectory()+"/DCIM/Camera/"+mFileName;
            binding.recordFilepath.setText(mFilePath);
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_DEFAULT);

            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_TITLE,"新文件"+mFileName);
            activityResultLauncher.launch(intent);

    }

    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        Bundle result=new Bundle();
        //InputStreamReader()
        result.putString("path",mFilePath);
        result.putParcelable("uri",fileUri);
        getParentFragmentManager().setFragmentResult("record",result);
    }
}
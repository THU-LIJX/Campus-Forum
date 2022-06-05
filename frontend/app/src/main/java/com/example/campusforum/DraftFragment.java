package com.example.campusforum;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.campusforum.databinding.FragmentDraftBinding;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DraftFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DraftFragment extends Fragment {
    private String TAG = "DraftFragment";
    private FragmentDraftBinding binding;
    private DraftAdapter adapter;
    private List<JSONObject> draftList;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private String draftPath;

    public DraftFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DraftFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DraftFragment newInstance(String param1, String param2) {
        DraftFragment fragment = new DraftFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentDraftBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        draftList = new ArrayList<>();
        draftPath = Environment.getExternalStorageDirectory() + "/Documents/Campus/";
        File dir = new File(draftPath);
        int userID=User.currentUser.userId;
        if (dir.exists()) {
            File[] array = dir.listFiles();
            for (int i = 0; i < array.length; i++) {
                try {
                    FileInputStream fileInputStream = new FileInputStream(array[i]);
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                    char buf[] = new char[10000];
                    int len = inputStreamReader.read(buf);
                    if (len <= 0) {
                        Log.d(TAG, "onViewCreated: 文件读取错误");
                        continue;
                    }
                    String text = new String(buf, 0, len);
                    JSONObject obj = new JSONObject(text);
                    //判断，只读取当前用户的
                    if(obj.getInt("id")!=userID){
                        continue;
                    }
                    Log.d(TAG, "onViewCreated: jsonObj:" + obj);
                    draftList.add(obj);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        Log.d(TAG, "onViewCreated: draftList size:" + draftList.size());
        adapter = new DraftAdapter(getContext(), draftList);
        binding.draftRecycler.setAdapter(adapter);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        binding.draftRecycler.setLayoutManager(manager);
        binding.draftRecycler.setEnabled(true);
        binding.draftRecycler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                int position = binding.draftRecycler.getChildAdapterPosition(view);
                Intent intent = new Intent(getContext(), PostEditActivity.class);

                intent.putExtra("draft", draftList.get(position).toString());
                startActivity(intent);
            }
        });
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        adapter.clear();
        File dir = new File(draftPath);
        int userID=User.currentUser.userId;
        if (dir.exists()) {
            File[] array = dir.listFiles();
            for (int i = 0; i < array.length; i++) {
                try {
                    FileInputStream fileInputStream = new FileInputStream(array[i]);
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                    char buf[] = new char[10000];
                    int len = inputStreamReader.read(buf);
                    if (len <= 0) {
                        Log.d(TAG, "onViewCreated: 文件读取错误");
                        continue;
                    }
                    String text = new String(buf, 0, len);
                    JSONObject obj = new JSONObject(text);
                    if(obj.getInt("id")!=userID){
                        continue;
                    }
                    Log.d(TAG, "onViewCreated: jsonObj:" + obj);
                    draftList.add(obj);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        adapter.notifyDataSetChanged();

        super.onResume();
    }

}
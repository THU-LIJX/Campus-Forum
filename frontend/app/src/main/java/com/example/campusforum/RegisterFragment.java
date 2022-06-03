package com.example.campusforum;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.campusforum.databinding.FragmentRegisterBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RegisterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RegisterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RegisterFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RegisterFragment newInstance(String param1, String param2) {
        RegisterFragment fragment = new RegisterFragment();
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
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
        binding.fragRegiBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String eml = binding.fragRegiEdtEmail.getText().toString();
                String usr = binding.fragRegiEdtUsername.getText().toString();
                String pwd = binding.fragRegiEdtPassword.getText().toString();

                HashMap<String, String> data = new HashMap<>();
                data.put("email", eml);
                data.put("name", usr);
                data.put("password", pwd);
                HttpUtil.sendPostRequest("/api/register", data, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        System.out.println(e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String result = Objects.requireNonNull(response.body()).string();
                        JSONObject jsonOb;
                        try {
                            jsonOb = new JSONObject(result);
                            System.out.println(jsonOb.toString());
                            // 注册成功：跳转至注册结果页面；注册失败：提示错误信息
                            if (jsonOb.getString("message").equals("ok")) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_register_fragment_to_register_result_fragment);
                                    }
                                });
                            } else {
                                // TODO: 提示错误信息
                            }
                        } catch (JSONException e) {
                            System.out.println(e);
                        }
                    }
                });
            }
        });
        binding.fragRegiBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_register_fragment_to_login_fragment);
            }
        });

    }
}
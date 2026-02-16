package com.example.mobile.ui.admin;

import com.example.mobile.R;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.os.Bundle;

import com.example.mobile.utils.NavbarHelper;

public class AdminReportsFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        // Navbar setup
        NavbarHelper.setup(this, root, "Reports");

        return root;
    }
}

/*
 * System Shell Box
 * Copyright (C) 2025-2026 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.manager.ssb.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;
import com.manager.ssb.model.WelcomePage;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class WelcomePagerAdapter extends RecyclerView.Adapter<WelcomePagerAdapter.BaseViewHolder> {

    public interface OnLicenseViewCreatedListener {
        void onLicenseViewCreated(View licenseView);
    }

    public interface OnPermissionViewCreatedListener {
        void onPermissionViewCreated(View permissionView);
    }

    private final List<WelcomePage> pages;
    private final OnLicenseViewCreatedListener licenseListener;
    private final OnPermissionViewCreatedListener permissionListener;

    public WelcomePagerAdapter(List<WelcomePage> pages, 
                             OnLicenseViewCreatedListener licenseListener,
                             OnPermissionViewCreatedListener permissionListener) {
        this.pages = pages;
        this.licenseListener = licenseListener;
        this.permissionListener = permissionListener;
    }

    @Override
    public int getItemViewType(int position) {
        return pages.get(position).getPageType();
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case WelcomePage.TYPE_LICENSE:
                View licenseView = inflater.inflate(R.layout.item_welcome_license, parent, false);
                return new LicenseViewHolder(licenseView);
                
            case WelcomePage.TYPE_PERMISSION:
                View permissionView = inflater.inflate(R.layout.item_welcome_permission, parent, false);
                return new PermissionViewHolder(permissionView);
                
            case WelcomePage.TYPE_COMPLETE:
                View completeView = inflater.inflate(R.layout.item_welcome_complete, parent, false);
                return new CompleteViewHolder(completeView);
                
            case WelcomePage.TYPE_STANDARD:
            default:
                View standardView = inflater.inflate(R.layout.item_welcome_standard, parent, false);
                return new StandardViewHolder(standardView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        WelcomePage page = pages.get(position);
        holder.bind(page);
        
        // 特殊页面的回调
        if (holder instanceof LicenseViewHolder && licenseListener != null) {
            licenseListener.onLicenseViewCreated(holder.itemView);
        } else if (holder instanceof PermissionViewHolder && permissionListener != null) {
            permissionListener.onPermissionViewCreated(holder.itemView);
        }
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    abstract static class BaseViewHolder extends RecyclerView.ViewHolder {
        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        
        public abstract void bind(WelcomePage page);
    }

    static class StandardViewHolder extends BaseViewHolder {
        private final ImageView ivIllustration;
        private final TextView tvTitle;
        private final TextView tvDescription;

        public StandardViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIllustration = itemView.findViewById(R.id.ivIllustration);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }

        @Override
        public void bind(WelcomePage page) {
            ivIllustration.setImageResource(page.getIllustrationRes());
            tvTitle.setText(page.getTitle());
            tvDescription.setText(page.getDescription());
        }
    }

    static class LicenseViewHolder extends BaseViewHolder {
        private final TextView tvTitle;
        private final ScrollView licenseScrollView;
        private final TextView tvLicenseText;
        private final RadioGroup radioGroupLanguage;
        private final RadioButton radioEnglish;
        private final RadioButton radioChinese;

        public LicenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            licenseScrollView = itemView.findViewById(R.id.licenseScrollView);
            tvLicenseText = itemView.findViewById(R.id.tvLicenseText);
            radioGroupLanguage = itemView.findViewById(R.id.radioGroupLanguage);
            radioEnglish = itemView.findViewById(R.id.radioEnglish);
            radioChinese = itemView.findViewById(R.id.radioChinese);
        }

        @Override
        public void bind(WelcomePage page) {
            tvTitle.setText(page.getTitle());
            // 具体的许可证文本加载在 Activity 中通过回调处理
        }
    }

    static class PermissionViewHolder extends BaseViewHolder {
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final LinearLayout permissionsContainer;
        private final MaterialButton btnGrantPermissions;
        private final TextView tvPermissionStatus;

        public PermissionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            permissionsContainer = itemView.findViewById(R.id.permissionsContainer);
            btnGrantPermissions = itemView.findViewById(R.id.btnGrantPermissions);
            tvPermissionStatus = itemView.findViewById(R.id.tvPermissionStatus);
        }

        @Override
        public void bind(WelcomePage page) {
            tvTitle.setText(page.getTitle());
            tvDescription.setText(page.getDescription());
            // 具体的权限列表和按钮处理在 Activity 中通过回调处理
        }
    }

    static class CompleteViewHolder extends BaseViewHolder {
        private final ImageView ivIcon;
        private final TextView tvMainTitle;
        private final TextView tvSubtitle;
        private final TextView tvDescription;

        public CompleteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvMainTitle = itemView.findViewById(R.id.tvMainTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }

        @Override
        public void bind(WelcomePage page) {
            ivIcon.setImageResource(page.getIllustrationRes());
            tvMainTitle.setText(page.getTitle());
            tvSubtitle.setText(page.getSubtitle());
            tvDescription.setText(page.getDescription());
        }
    }
}
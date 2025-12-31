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

package com.manager.ssb.model;

public class WelcomePage {
    public static final int TYPE_STANDARD = 0;
    public static final int TYPE_LICENSE = 1;
    public static final int TYPE_PERMISSION = 2;
    public static final int TYPE_COMPLETE = 3;
    
    private final int illustrationRes;
    private final String title;
    private final String description;
    private final int pageType;
    private final String subtitle;

    public WelcomePage(int illustrationRes, String title, String description, int pageType) {
        this.illustrationRes = illustrationRes;
        this.title = title;
        this.description = description;
        this.pageType = pageType;
        this.subtitle = "";
    }

    public WelcomePage(int illustrationRes, String title, String subtitle, String description, int pageType) {
        this.illustrationRes = illustrationRes;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.pageType = pageType;
    }

    public int getIllustrationRes() { return illustrationRes; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getPageType() { return pageType; }
    public String getSubtitle() { return subtitle; }
    public boolean isLicensePage() { return pageType == TYPE_LICENSE; }
    public boolean isPermissionPage() { return pageType == TYPE_PERMISSION; }
    public boolean isCompletePage() { return pageType == TYPE_COMPLETE; }
    public boolean isStandardPage() { return pageType == TYPE_STANDARD; }
}
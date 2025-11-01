/*
 * System Shell Box
 * Copyright (C) 2025 kgultrt
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
    private final int illustrationRes;
    private final String title;
    private final String description;
    private final boolean requiresAction;

    public WelcomePage(int illustrationRes, String title, String description, boolean requiresAction) {
        this.illustrationRes = illustrationRes;
        this.title = title;
        this.description = description;
        this.requiresAction = requiresAction;
    }

    public int getIllustrationRes() { return illustrationRes; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean requiresAction() { return requiresAction; }
}
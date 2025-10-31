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
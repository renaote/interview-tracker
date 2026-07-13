package com.renate.tracker.model;

/**
 * Represents the stage of an application in the interview pipeline.
 * Order matters: ordinal() is used to drive the dashboard funnel chart
 * and the "advance stage" button, which just moves to the next one in
 * this list.
 */
public enum Stage {
    WISHLIST,
    APPLIED,
    ASSESSMENT,
    INTERVIEW,
    OFFER,
    REJECTED;

    @Override
    public String toString() {
        String s = name();
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
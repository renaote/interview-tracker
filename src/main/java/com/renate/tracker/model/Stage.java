package com.renate.tracker.model;

// The five stages an application can be in.
// I used an enum instead of just a String so I can't accidentally
// misspell a stage name somewhere and break the whole app.
public enum Stage {
    APPLIED,
    ASSESSMENT,
    INTERVIEW,
    OFFER,
    REJECTED;

    // Makes the dropdown show "Applied" instead of "APPLIED"
    @Override
    public String toString() {
        String s = name();
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
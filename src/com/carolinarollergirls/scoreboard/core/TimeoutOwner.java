package com.carolinarollergirls.scoreboard.core;

public interface TimeoutOwner {
    public String getId();

    public void setInTimeout(boolean in_timeout);
    public void setInOfficialReview(boolean in_official_review);
}

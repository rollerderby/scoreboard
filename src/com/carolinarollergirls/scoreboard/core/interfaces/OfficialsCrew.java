package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface OfficialsCrew extends ScoreBoardEventProvider {
    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
    public static final Value<Member> HEAD_NSO = new Value<>(Member.class, "HNSO", null, props);
    public static final Value<Member> HEAD_REF = new Value<>(Member.class, "HR", null, props);

    public static final Child<Member> NSO = new Child<>(Member.class, "Nso", props);
    public static final Child<Member> REF = new Child<>(Member.class, "Ref", props);

    public static interface Member extends ScoreBoardEventProvider {
        public Child<Member> getType();

        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> ROLE = new Value<>(String.class, "Role", "", props);
        @SuppressWarnings("hiding")
        public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
        public static final Value<String> LEAGUE = new Value<>(String.class, "League", "", props);
        public static final Value<String> CERT = new Value<>(String.class, "Cert", "", props);
        public static final Value<PreparedOfficial> PREPARED_OFFICIAL =
            new Value<>(PreparedOfficial.class, "PreparedOfficial", null, props);
    }
}
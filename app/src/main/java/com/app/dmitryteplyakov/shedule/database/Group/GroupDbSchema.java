package com.app.dmitryteplyakov.shedule.database.Group;

/**
 * Created by dmitry21 on 25.08.17.
 */

public class GroupDbSchema {
    public static final class GroupTable {
        public static final String NAME = "groups";

        public static final class Cols {
            public static final String FACULTY = "faculty";
            public static final String SPEC = "spec";
            public static final String LINK = "link";
            public static final String TITLE = "title";
            public static final String UUID = "discipline_uuid";
        }
    }
}

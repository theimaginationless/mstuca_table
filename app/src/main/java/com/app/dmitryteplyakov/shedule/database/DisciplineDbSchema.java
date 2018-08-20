package com.app.dmitryteplyakov.shedule.database;

/**
 * Created by dmitry21 on 25.08.17.
 */

public class DisciplineDbSchema {
    public static final class DisciplineTable {
        public static final String NAME = "disciplines";

        public static final class Cols {
            public static final String TEACHERNAME = "discipline_teacher_name";
            public static final String DISCIPLINETITLE = "discipline_title";
            public static final String AUDNUMBER = "discipline_auditory_number";
            public static final String DATE = "discipline_date";
            public static final String UUID = "discipline_uuid";
            public static final String TYPE = "discipline_type";
            public static final String NUMBER = "discipline_number";
        }
    }
}

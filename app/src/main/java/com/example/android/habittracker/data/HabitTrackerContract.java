package com.example.android.habittracker.data;

import android.provider.BaseColumns;

public class HabitTrackerContract {

    public HabitTrackerContract() {}

    public static class HabitEntry implements BaseColumns {
        // Table name
        public static final String TABLE_NAME = "habits";

        // Columns
        public static final String COLUMN_ID = BaseColumns._ID;
        public static final String COLUMN_WEEK = "week";
        public static final String COLUMN_DAY = "weekday";
        public static final String COLUMN_JOGGING_TIME = "jogging";
        public static final String COLUMN_SWIMMING_TIME = "swimming";
        public static final String COLUMN_GRANMA_TIME = "granma";
        public static final String COLUMN_FRENCH_TIME = "french";

        // Other constants
        public static final int NONE = 0;
        public static final int HALF_HOUR = 1;
        public static final int ONE_HOUR = 2;
        public static final int ONE_HOUR_AND_HALF = 3;
        public static final int TWO_HOURS = 4;

        public static final int MON = 1;
        public static final int TUE = 2;
        public static final int WED = 3;
        public static final int THU = 4;
        public static final int FRI = 5;
        public static final int SAT = 6;
        public static final int SUN = 7;
    }
}

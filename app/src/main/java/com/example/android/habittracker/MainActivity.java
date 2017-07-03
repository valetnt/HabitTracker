package com.example.android.habittracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.android.habittracker.data.HabitTrackerContract.HabitEntry;
import com.example.android.habittracker.data.HabitTrackerDbHelper;

/*
 * This activity allows the user to keep track daily of the number of hours he has spent
 * doing a certain activity, such as swimming, jogging, or studying French,
 * or keep track of his voluntary work tutoring young students with their homework.
 * NO CALENDARS have been used for the moment, for the sake of simplicity.
 * It is simply assumed that the app IS USED EVERY DAY AND ONLY ONCE PER DAY,
 * so that when you relaunch the app -- and so this activity is recreated --
 * the weekday that is displayed at the top of the screen is simply the next day.
 *
 */
public class MainActivity extends AppCompatActivity {

    HabitTrackerDbHelper mDbHelper;

    // Spinners for selecting the number of hours spent doing each activity:
    Spinner mSpinnerJogging;
    Spinner mSpinnerSwimming;
    Spinner mSpinnerFrench;

    EditText mStudent;

    // There is a button to confirm that data has been entered correctly,
    // and that we want the database to update with this new piece of data.
    // After you have confirmed, you WILL NOT be able to edit data!
    // In other words, you are done for the current day.
    Button mButtonOk;

    /*
     * To make things simple (so we don't need to use calendars for the moment),
     * when we install the app, the weekday is initially set to MONDAY
     * and the week is initially set to WEEK 1.
     * Then, if the activity is recreated, the day that is going to be displayed
     * is the next day (i.e. Tuesday of week 1).
     * And so on, every time the activity is recreated, the weekday increments by 1,
     * and once we reach Sunday, it starts again from day 1 (Monday) and the week increments by 1.
     */
    int mCurrentWeek;
    int mCurrentWeekday;

    // There is also a button that allows to skip directly to the next day.
    // This is just for convenience, to make it quicker to test app behaviour!
    // This button has the same effect as closing and then relaunching the app.
    Button mButtonNextDay;

    TextView mWeekday; // To be displayed at the top of the screen

    LinearLayout mSummaryTable; // A summary of the current week

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHelper = new HabitTrackerDbHelper(this);

        mStudent = (EditText) findViewById(R.id.student);

        mSpinnerJogging = (Spinner) findViewById(R.id.spinner_jogging);
        mSpinnerSwimming = (Spinner) findViewById(R.id.spinner_swimming);
        mSpinnerFrench = (Spinner) findViewById(R.id.spinner_french);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_time_options, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the same adapter to all the spinners
        mSpinnerJogging.setAdapter(adapter);
        mSpinnerSwimming.setAdapter(adapter);
        mSpinnerFrench.setAdapter(adapter);

        mWeekday = (TextView) findViewById(R.id.weekday);

        // Update day (and possibly week) every time this activity is recreated
        updateWeekDay();

        mSummaryTable = (LinearLayout) findViewById(R.id.container);
        displaySummary();

        mButtonNextDay = (Button) findViewById(R.id.button_next_day);
        mButtonNextDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset the spinners
                mSpinnerJogging.setEnabled(true);
                mSpinnerSwimming.setEnabled(true);
                mSpinnerFrench.setEnabled(true);
                mSpinnerJogging.setSelection(0);
                mSpinnerSwimming.setSelection(0);
                mSpinnerFrench.setSelection(0);
                // Reset EditText
                mStudent.setEnabled(true);
                mStudent.setText("");

                // If a new week starts, clean the summary table
                if (mCurrentWeekday == HabitEntry.SUN) {
                    mSummaryTable.removeAllViews();
                }

                // Update day (and possibly week)
                updateWeekDay();

                // Reactivate the "CONFIRM" button
                mButtonOk.setEnabled(true);

                // Disable the "NEXT DAY" button until the database is updated
                // with the data of the current day
                mButtonNextDay.setEnabled(false);
            }
        });

        // Disable the "NEXT DAY" button until the database is updated
        // with the data of the current day
        mButtonNextDay.setEnabled(false);

        mButtonOk = (Button) findViewById(R.id.button_ok);
        mButtonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Store the number of hours spent doing each activity for the current day
                // into an integer variable, reading from the spinner selected item
                int todayJogging = getSelectedInteger(mSpinnerJogging.getSelectedItemPosition());
                int todaySwimming = getSelectedInteger(mSpinnerSwimming.getSelectedItemPosition());
                int todayFrench = getSelectedInteger(mSpinnerFrench.getSelectedItemPosition());

                // Store student's name, reading from the editable field
                String todayStudent = String.valueOf(mStudent.getText());

                // Update database
                updateDatabase(HabitEntry.TABLE_NAME, todayJogging, todaySwimming, todayStudent,
                        todayFrench);

                // Update summary table with the newly entered data
                addToSummary(mCurrentWeekday, todayJogging, todaySwimming, todayStudent, todayFrench);

                // Make it impossible for the user to hit "CONFIRM" more than once
                mButtonOk.setEnabled(false);

                // Enable the "NEXT DAY" button
                mButtonNextDay.setEnabled(true);

                // Disable the spinners, since data is no longer editable
                mSpinnerJogging.setEnabled(false);
                mSpinnerSwimming.setEnabled(false);
                mSpinnerFrench.setEnabled(false);
                // Disable EditText
                mStudent.setEnabled(false);
            }
        });
    }

    /**
     * Method that inserts a new entry into the the specified table.
     * The entry is inserted with the current week and weekday values.
     *
     * @param table:  (string) name of the table that has to be updated
     * @param value1: (integer) content of column {@link HabitEntry#COLUMN_JOGGING_TIME}
     * @param value2: (integer) content of column {@link HabitEntry#COLUMN_SWIMMING_TIME}
     * @param value3: (string) content of column {@link HabitEntry#COLUMN_TUTORING}
     * @param value4: (integer) content of column {@link HabitEntry#COLUMN_FRENCH_TIME}
     */
    private void updateDatabase(String table, int value1, int value2, String value3, int value4) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HabitEntry.COLUMN_WEEK, mCurrentWeek);
        values.put(HabitEntry.COLUMN_DAY, mCurrentWeekday);
        values.put(HabitEntry.COLUMN_JOGGING_TIME, value1);
        values.put(HabitEntry.COLUMN_SWIMMING_TIME, value2);
        values.put(HabitEntry.COLUMN_TUTORING, value3);
        values.put(HabitEntry.COLUMN_FRENCH_TIME, value4);
        db.insert(table, null, values);
    }

    /**
     * Method that performs a database query, returning results in the default sort order.
     * It queries the database using the following SQL statement:
     * <p>
     * "SELECT column0, column1, ...
     * FROM table
     * WHERE (key0, key1, ...) = (selectionArgs[0], selectionArgs[1], ...);"
     *
     * @param table:         the table name to compile the query against
     * @param columns:       a list of which columns to return
     * @param selection:     a filter declaring which rows to return, formatted as an SQL WHERE clause
     *                       like the one above (but excluding the "WHERE" itself).
     *                       Passing null will return all rows for the given table.
     * @param selectionArgs: the array of values - expressed as strings - to be assigned to the
     *                       keys in {@param selection}, as in the SQL WHERE clause above,
     *                       or null if {@param selection} is null.
     * @return a {@link Cursor} object, which is positioned before the first entry.
     */
    private Cursor getCursor(String table, String[] columns, String selection,
                             String[] selectionArgs) {

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        return db.query(table, columns, selection, selectionArgs, null, null, null);
    }

    /**
     * Helper method that updates mCurrentWeekday (and possibly mCurrentWeek).
     * It is invoked every time the activity is recreated, or when the button "NEXT DAY" is clicked.
     */
    private void updateWeekDay() {

        // Query database to retrieve day and week of the last entry
        String[] columns = {HabitEntry.COLUMN_WEEK, HabitEntry.COLUMN_DAY};
        Cursor cursor = getCursor(HabitEntry.TABLE_NAME, columns, null, null);
        try {

            if (cursor.getCount() == 0) {
                // Initialize the week index the very first time this app is used
                mCurrentWeek = 1;
                // Let's assume that we start using this app for the first time on a Monday
                mCurrentWeekday = HabitEntry.MON;

            } else {
                // If it is not the first time we use the app, then check
                // when it was the last time it's been used and increment day by 1.
                // For the moment, we are under the simplified assumption that the app
                // is used EVERY DAY AND ONLY ONCE PER DAY. No calendars have been used,
                // in order to keep things as simple as possible.
                cursor.moveToLast();
                int weekLastUsed = cursor.getInt(cursor.getColumnIndex(HabitEntry.COLUMN_WEEK));
                int dayLastUsed = cursor.getInt(cursor.getColumnIndex(HabitEntry.COLUMN_DAY));
                if (dayLastUsed == HabitEntry.SUN) {
                    // If last time was a Sunday, today it is the Monday of the next week
                    mCurrentWeek = weekLastUsed + 1;
                    mCurrentWeekday = HabitEntry.MON;
                } else {
                    mCurrentWeek = weekLastUsed;
                    mCurrentWeekday = dayLastUsed + 1;
                }
            }

            mWeekday.setText(getWeekday(mCurrentWeekday, 2));

        } finally {
            cursor.close();
        }
    }

    /**
     * Helper method that returns the weekday name to be displayed in the summary table
     *
     * @param constant: one of the constants defined in {@link HabitTrackerDbHelper}
     * @param flag:     if flag == 1, it returns the abbreviated version of the name of the weekday;
     *                  if flag == 2, it returns the full name.
     */
    private String getWeekday(int constant, int flag) {
        switch (flag) {
            case 1:
                if (constant == HabitEntry.MON) {
                    return "Mon";
                } else if (constant == HabitEntry.TUE) {
                    return "Tue";
                } else if (constant == HabitEntry.WED) {
                    return "Wed";
                } else if (constant == HabitEntry.THU) {
                    return "Thu";
                } else if (constant == HabitEntry.FRI) {
                    return "Fri";
                } else if (constant == HabitEntry.SAT) {
                    return "Sat";
                } else if (constant == HabitEntry.SUN) {
                    return "Sun";
                }

            case 2:
                if (constant == HabitEntry.MON) {
                    return "Monday";
                } else if (constant == HabitEntry.TUE) {
                    return "Tuesday";
                } else if (constant == HabitEntry.WED) {
                    return "Wednesday";
                } else if (constant == HabitEntry.THU) {
                    return "Thursday";
                } else if (constant == HabitEntry.FRI) {
                    return "Friday";
                } else if (constant == HabitEntry.SAT) {
                    return "Saturday";
                } else if (constant == HabitEntry.SUN) {
                    return "Sunday";
                }

            default:
                return null;
        }
    }

    // Helper method that returns the integer value corresponding to the item
    // selected in the spinner dropdown menu
    private int getSelectedInteger(int position) {
        switch (position) {
            case 1:
                return HabitEntry.NONE;
            case 2:
                return HabitEntry.HALF_HOUR;
            case 3:
                return HabitEntry.ONE_HOUR;
            case 4:
                return HabitEntry.ONE_HOUR_AND_HALF;
            case 5:
                return HabitEntry.TWO_HOURS;
        }
        return 0;
    }

    // Helper method to update and display the summary table for the current week
    private void displaySummary() {

        // Build up the table retrieving data from database
        String[] columns = {HabitEntry.COLUMN_DAY, HabitEntry.COLUMN_JOGGING_TIME,
                HabitEntry.COLUMN_SWIMMING_TIME, HabitEntry.COLUMN_TUTORING,
                HabitEntry.COLUMN_FRENCH_TIME};
        // We are only interested in displaying the current week
        String selection = HabitEntry.COLUMN_WEEK + " =? ";
        String selectionArgs[] = {String.valueOf(mCurrentWeek)};
        Cursor cursor = getCursor(HabitEntry.TABLE_NAME, columns, selection, selectionArgs);
        try {
            if (cursor.getCount() > 0) {

                // Loop on all the table entries, from Monday till today
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {

                    // First, inflate the row layout from a resource file
                    LinearLayout new_entry = (LinearLayout)
                            LayoutInflater.from(this).inflate(R.layout.table_entries,
                                    mSummaryTable, false);

                    mSummaryTable.addView(new_entry);

                    // Now, fill the row
                    // Zeroth column: the day of the week
                    int weekday = cursor.getInt(cursor.getColumnIndex(HabitEntry.COLUMN_DAY));
                    ((TextView) new_entry.findViewById(R.id.weekday)).setText(getWeekday(weekday, 1));

                    // First, second and fourth columns:
                    // the number of hours spent doing each activity.
                    // Retrieve this number parsing the cursor, then turn it into a string.
                    // Remember that HALF_HOUR = 1

                    // First column
                    int hours_jogging = cursor.getInt(cursor.getColumnIndex
                            (HabitEntry.COLUMN_JOGGING_TIME));
                    ((TextView) new_entry.findViewById(R.id.column_jogging)).setText
                            (String.valueOf(hours_jogging * 0.5));
                    // Second column
                    int hours_swimming = cursor.getInt(cursor.getColumnIndex
                            (HabitEntry.COLUMN_SWIMMING_TIME));
                    ((TextView) new_entry.findViewById(R.id.column_swimming)).setText
                            (String.valueOf(hours_swimming * 0.5));
                    // Third column: tutored student's name
                    String student = cursor.getString(cursor.getColumnIndex
                            (HabitEntry.COLUMN_TUTORING));
                    ((TextView) new_entry.findViewById(R.id.column_tutoring)).setText(student);
                    // Fourth column
                    int hours_french = cursor.getInt(cursor.getColumnIndex
                            (HabitEntry.COLUMN_FRENCH_TIME));
                    ((TextView) new_entry.findViewById(R.id.column_french)).setText
                            (String.valueOf(hours_french * 0.5));

                    cursor.moveToNext();
                }
            }

        } finally {
            cursor.close();
        }
    }

    // Helper method to add the newly entered data to the summary table
    private void addToSummary(int value1, int value2, int value3, String value4, int value5) {

        // First, inflate the row layout from a resource file
        LinearLayout new_entry = (LinearLayout)
                LayoutInflater.from(this).inflate(R.layout.table_entries,
                        mSummaryTable, false);

        mSummaryTable.addView(new_entry);

        // Now, fill the row
        // Zeroth column: the day of the week
        ((TextView) new_entry.findViewById(R.id.weekday)).setText(getWeekday(value1, 1));

        // First, second and fourth columns:
        // the number of hours spent doing each activity.
        // Remember that HALF_HOUR = 1

        // First column
        ((TextView) new_entry.findViewById(R.id.column_jogging)).setText
                (String.valueOf(value2 * 0.5));
        // Second column
        ((TextView) new_entry.findViewById(R.id.column_swimming)).setText
                (String.valueOf(value3 * 0.5));
        // Third column: tutored student's name
        ((TextView) new_entry.findViewById(R.id.column_tutoring)).setText(value4);
        // Fourth column
        ((TextView) new_entry.findViewById(R.id.column_french)).setText
                (String.valueOf(value5 * 0.5));
    }
}

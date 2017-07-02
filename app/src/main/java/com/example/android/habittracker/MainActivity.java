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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.android.habittracker.data.HabitTrackerContract.HabitEntry;
import com.example.android.habittracker.data.HabitTrackerDbHelper;

public class MainActivity extends AppCompatActivity {

    HabitTrackerDbHelper mDbHelper;
    Spinner mSpinnerJogging;
    Spinner mSpinnerSwimming;
    Spinner mSpinnerGranma;
    Spinner mSpinnerFrench;
    TextView mWeekday;
    int mCurrentWeek;
    Button mButtonNextDay;
    Button mButtonOk;
    LinearLayout mSummaryTable;

    // For the moment, we don't have an editor, so the user can only enter data once.
    // Make sure that he can hit "CONFIRM" button ONLY ONCE.
    boolean mIsConfirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHelper = new HabitTrackerDbHelper(this);

        mSpinnerJogging = (Spinner) findViewById(R.id.spinner_jogging);
        mSpinnerSwimming = (Spinner) findViewById(R.id.spinner_swimming);
        mSpinnerGranma = (Spinner) findViewById(R.id.spinner_granma);
        mSpinnerFrench = (Spinner) findViewById(R.id.spinner_french);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_time_options, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the same adapter to all the spinners
        mSpinnerJogging.setAdapter(adapter);
        mSpinnerSwimming.setAdapter(adapter);
        mSpinnerGranma.setAdapter(adapter);
        mSpinnerFrench.setAdapter(adapter);

        mWeekday = (TextView) findViewById(R.id.weekday);
        mWeekday.setText(getString(R.string.monday));

        // Update week index
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] columns = {HabitEntry.COLUMN_WEEK};
        Cursor cursor = db.query(HabitEntry.TABLE_NAME, columns, null, null, null, null,
                null);
        try {
            if (cursor.getCount() == 0) {
                // Initialize the week index the very first time this app is used
                mCurrentWeek = 1;
            } else {
                cursor.moveToLast();
                mCurrentWeek = cursor.getInt(cursor.getColumnIndex(HabitEntry.COLUMN_WEEK));
            }
        } finally {
            cursor.close();
        }


        mSummaryTable = (LinearLayout) findViewById(R.id.table);
        displaySummary();

        mButtonNextDay = (Button) findViewById(R.id.button_next_day);
        mButtonNextDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Reset the spinners
                mSpinnerJogging.setSelection(0);
                mSpinnerSwimming.setSelection(0);
                mSpinnerGranma.setSelection(0);
                mSpinnerFrench.setSelection(0);

                // Update the day of the week
                if(mWeekday.getText().toString().equals(getString(R.string.monday))) {
                    mWeekday.setText(getString(R.string.tuesday));
                } else if(mWeekday.getText().toString().equals(getString(R.string.tuesday))) {
                    mWeekday.setText(getString(R.string.wednesday));
                } else if(mWeekday.getText().toString().equals(getString(R.string.wednesday))) {
                    mWeekday.setText(getString(R.string.thursday));
                } else if(mWeekday.getText().toString().equals(getString(R.string.thursday))) {
                    mWeekday.setText(getString(R.string.friday));
                } else if(mWeekday.getText().toString().equals(getString(R.string.friday))) {
                    mWeekday.setText(getString(R.string.saturday));
                } else if(mWeekday.getText().toString().equals(getString(R.string.saturday))) {
                    mWeekday.setText(getString(R.string.sunday));
                } else if(mWeekday.getText().toString().equals(getString(R.string.sunday))) {
                    mWeekday.setText(getString(R.string.monday));
                    // With the beginning of a new week, increment the number of the week by 1
                    int lastWeek;
                    SQLiteDatabase db = mDbHelper.getReadableDatabase();
                    String[] columns = {HabitEntry.COLUMN_WEEK};
                    Cursor cursor = db.query(HabitEntry.TABLE_NAME, columns, null, null, null, null,
                            null);
                    try {
                        cursor.moveToLast(); // Read last week index
                        lastWeek = cursor.getInt(cursor.getColumnIndex(HabitEntry.COLUMN_WEEK));
                    } finally {
                        cursor.close();
                    }
                    // Increment week number by 1
                    mCurrentWeek = lastWeek + 1;
                }
            }
        });

        mButtonOk = (Button) findViewById(R.id.button_ok);
        mButtonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mIsConfirmed) {
                    // Make it impossible for the user to hit "CONFIRM" more than once
                    mIsConfirmed = true;

                    // Store the number of hours spent doing each activity for the current day
                    // into an integer variable, reading from the spinner selected items
                    int todayJogging = getSelectedInteger(mSpinnerJogging.getSelectedItemPosition());
                    int todaySwimming = getSelectedInteger(mSpinnerSwimming.getSelectedItemPosition());
                    int todayGranma = getSelectedInteger(mSpinnerGranma.getSelectedItemPosition());
                    int todayFrench = getSelectedInteger(mSpinnerFrench.getSelectedItemPosition());

                    // Update database
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(HabitEntry.COLUMN_WEEK, mCurrentWeek);
                    values.put(HabitEntry.COLUMN_JOGGING_TIME, todayJogging);
                    values.put(HabitEntry.COLUMN_SWIMMING_TIME, todaySwimming);
                    values.put(HabitEntry.COLUMN_GRANMA_TIME, todayGranma);
                    values.put(HabitEntry.COLUMN_FRENCH_TIME, todayFrench);
                    db.insert(HabitEntry.TABLE_NAME, null, values);

                    // Update summary table with the newly entered data
                    displaySummary();
                }
            }
        });
    }

    // Helper method that returns the integer value corresponding to the item
    // selected in the spinner dropdown menu
    private int getSelectedInteger(int position) {
        switch(position) {
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

    // Helper method to update and display the summary table
    private void displaySummary() {
        // Build up the table retrieving data from database
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] columns = {HabitEntry.COLUMN_JOGGING_TIME, HabitEntry.COLUMN_SWIMMING_TIME,
                HabitEntry.COLUMN_GRANMA_TIME, HabitEntry.COLUMN_FRENCH_TIME};
        String selection = "WHERE " + HabitEntry.COLUMN_WEEK + " =? ";
        String selectionArgs[] = {String.valueOf(mCurrentWeek)};
        Cursor cursor = db.query(HabitEntry.TABLE_NAME, columns, selection, selectionArgs, null,
                null, null);
        try {
            if(cursor.getCount() > 0) {
                // If there is at least one row, make the table visible
                mSummaryTable.setVisibility(View.VISIBLE);

                // Loop on all the table entries
                cursor.moveToFirst();
                while(!cursor.isAfterLast()) {

                    // First, inflate the row layout from a resource file
                    LinearLayout new_entry = (LinearLayout)
                            LayoutInflater.from(this).inflate(R.layout.table_entries,
                                    mSummaryTable, false);
                    // Attach the row to the already existing table
                    mSummaryTable.addView(new_entry);

                    // Now, fill the row
                    // Zeroth column: the day of the week
                    ((TextView) new_entry.findViewById(R.id.weekday)).setText
                            (abbreviateWeekday(mWeekday.getText().toString()));

                    // First, second, third and fourth columns:
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
                    // Third column
                    int hours_granma = cursor.getInt(cursor.getColumnIndex
                            (HabitEntry.COLUMN_GRANMA_TIME));
                    ((TextView) new_entry.findViewById(R.id.column_granma)).setText
                            (String.valueOf(hours_granma * 0.5));
                    // Fourth column
                    int hours_french = cursor.getInt(cursor.getColumnIndex
                            (HabitEntry.COLUMN_FRENCH_TIME));
                    ((TextView) new_entry.findViewById(R.id.column_french)).setText
                            (String.valueOf(hours_french * 0.5));

                    cursor.moveToNext();
                }
            } else {
                // Table remains empty, so hide it
                mSummaryTable.setVisibility(View.GONE);
            }

        } finally {
            cursor.close();
        }
    }

    // Helper method to abbreviate weekday (e.g. Monday --> Mon)
    private String abbreviateWeekday(String weekday) {
        if(weekday.equals(getString(R.string.monday))) {
            return getString(R.string.mon);
        } else if(weekday.equals(getString(R.string.tuesday))) {
            return getString(R.string.tue);
        } else if(weekday.equals(getString(R.string.wednesday))) {
            return getString(R.string.wen);
        } else if(weekday.equals(getString(R.string.thursday))) {
            return getString(R.string.thu);
        } else if(weekday.equals(getString(R.string.friday))) {
            return getString(R.string.fri);
        } else if(weekday.equals(getString(R.string.saturday))) {
            return getString(R.string.sat);
        } else if(weekday.equals(getString(R.string.sunday))) {
            return getString(R.string.sun);
        } else {
            return null;
        }
    }
}

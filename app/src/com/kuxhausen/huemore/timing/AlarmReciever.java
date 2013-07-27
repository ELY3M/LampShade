package com.kuxhausen.huemore.timing;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kuxhausen.huemore.R;
import com.kuxhausen.huemore.network.SynchronousTransmitGroupMood;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.GroupColumns;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.InternalArguments;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.MoodColumns;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.PreferencesKeys;
import com.kuxhausen.huemore.state.api.BulbState;

public class AlarmReciever extends BroadcastReceiver {

	Gson gson = new Gson();

	/**
	 * when this method is called, AlarmState as must have the correct hour and
	 * minute for each time the alarm is to be scheduled for
	 **/
	public static AlarmState createAlarms(Context context, AlarmState as) {
		Calendar soonestTime = null;

		if (!as.isRepeating()) {
			Calendar timeAdjustedCal = Calendar.getInstance();
			timeAdjustedCal.setTimeInMillis(as.getTime());
			timeAdjustedCal.setLenient(true);

			while (timeAdjustedCal.before(Calendar.getInstance())) {
				// make sure this hour & minute is in the future
				timeAdjustedCal.add(Calendar.DATE, 1);
			}
			as.setTime(timeAdjustedCal.getTimeInMillis());

			Log.e("asdf", "oneOffAlarm");

			AlarmReciever.scheduleAlarm(context, as,
					timeAdjustedCal.getTimeInMillis());
			soonestTime = timeAdjustedCal;
		} else {
			Calendar rightNow = Calendar.getInstance();
			long[] scheduledTimes = new long[7];
			for (int i = 0; i < 7; i++) {
				if (as.getRepeatingDays()[i]) {
					Calendar existingTimeCal = Calendar.getInstance();
					existingTimeCal.setTimeInMillis(as.getRepeatingTimes()[i]);

					Calendar copyForDayOfWeek = Calendar.getInstance();
					copyForDayOfWeek.setLenient(true);
					copyForDayOfWeek.set(Calendar.HOUR_OF_DAY,
							existingTimeCal.get(Calendar.HOUR_OF_DAY));
					copyForDayOfWeek.set(Calendar.MINUTE,
							existingTimeCal.get(Calendar.MINUTE));
					copyForDayOfWeek.set(Calendar.SECOND,
							existingTimeCal.get(Calendar.SECOND));

					/**
					 * 7+ desired day of week (+1 to convert to java calendar
					 * number) - current day of week %7
					 **/
					copyForDayOfWeek
							.add(Calendar.DATE, (7 + (1 + i) - rightNow
									.get(Calendar.DAY_OF_WEEK)) % 7);

					while (copyForDayOfWeek.before(Calendar.getInstance())) {
						// if in past, choose that day next week
						copyForDayOfWeek.add(Calendar.DATE, 7);
					}
					scheduledTimes[i] = copyForDayOfWeek.getTimeInMillis();
				}
			}
			as.setRepeatingTimes(scheduledTimes);

			for (int i = 0; i < 7; i++) {
				long t = as.getRepeatingTimes()[i];
				if (as.getRepeatingDays()[i]) {
					Log.e("asdf", "repeatingAlarm");
					AlarmReciever.scheduleWeeklyAlarm(context, as, t, i + 1);

					Calendar setTime = Calendar.getInstance();
					setTime.setTimeInMillis(t);
					if (soonestTime == null || setTime.before(soonestTime))
						soonestTime = setTime;
				}
			}
		}

		Toast.makeText(
				context,
				context.getString(R.string.next_scheduled_intro)
						+ " "
						+ DateUtils.getRelativeTimeSpanString(soonestTime
								.getTimeInMillis()), Toast.LENGTH_SHORT).show();
		return as;
	}

	private static void scheduleAlarm(Context context, AlarmState alarmState,
			Long timeInMillis) {

		Log.d("asdf",
				"createAlarm"
						+ ((timeInMillis - System.currentTimeMillis()) / 60000));

		PendingIntent pIntent = calculatePendingIntent(context, alarmState, 0);
		AlarmManager alarmMgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, timeInMillis, pIntent);
	}

	private static void scheduleWeeklyAlarm(Context context,
			AlarmState alarmState, Long timeInMillis, int dayOfWeek) {

		Log.d("asdf",
				"createRepeatingAlarm"
						+ ((timeInMillis - System.currentTimeMillis()) / 60000));

		PendingIntent pIntent = calculatePendingIntent(context, alarmState,
				dayOfWeek);
		AlarmManager alarmMgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, timeInMillis,
				AlarmManager.INTERVAL_DAY * 7, pIntent);
	}

	public static void cancelAlarm(Context context, AlarmState alarmState) {
		for (int i = 0; i < 8; i++) {
			PendingIntent pIntent = calculatePendingIntent(context, alarmState,
					8);
			AlarmManager alarmMgr = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			alarmMgr.cancel(pIntent);
		}
	}

	/** day of week Sunday = 1, Saturday = 7, 0=not repeating so we don't care **/
	private static PendingIntent calculatePendingIntent(Context context,
			AlarmState alarmState, int dayOfWeek) {
		Gson gson = new Gson();
		String aState = gson.toJson(alarmState);

		Intent intent = new Intent(context, AlarmReciever.class);
		intent.setAction("com.kuxhausen.huemore." + aState);
		intent.putExtra(InternalArguments.ALARM_DETAILS, aState);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				dayOfWeek, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pendingIntent;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != null) {
			AlarmState as = gson.fromJson(
					intent.getExtras().getString(
							InternalArguments.ALARM_DETAILS), AlarmState.class);

			// Look up bulbs for that mood from database
			String[] groupColumns = { GroupColumns.BULB };
			String[] gWhereClause = { as.group };
			Cursor groupCursor = context.getContentResolver()
					.query(DatabaseDefinitions.GroupColumns.GROUPBULBS_URI,
							groupColumns, GroupColumns.GROUP + "=?",
							gWhereClause, null);

			ArrayList<Integer> groupStates = new ArrayList<Integer>();
			while (groupCursor.moveToNext()) {
				groupStates.add(groupCursor.getInt(0));
			}
			Integer[] bulbS = groupStates.toArray(new Integer[groupStates
					.size()]);

			String[] moodColumns = { MoodColumns.STATE };
			String[] mWereClause = { as.mood };
			Cursor moodCursor = context.getContentResolver()
					.query(DatabaseDefinitions.MoodColumns.MOODSTATES_URI,
							moodColumns, MoodColumns.MOOD + "=?",
							mWereClause, null);

			ArrayList<String> moodStates = new ArrayList<String>();
			while (moodCursor.moveToNext()) {
				moodStates.add(moodCursor.getString(0));
			}
			String[] moodS = moodStates.toArray(new String[moodStates.size()]);
			
			int brightness = as.brightness;
			int transitiontime = as.transitiontime;
			for (int i = 0; i < moodS.length; i++) {
				BulbState bs = gson.fromJson(moodS[i], BulbState.class);
				bs.bri = brightness;
				bs.transitiontime = transitiontime;
				moodS[i] = gson.toJson(bs);// back into json for
											// TransmitGroupMood
			}

			SynchronousTransmitGroupMood trasmitter = new SynchronousTransmitGroupMood();
			trasmitter.execute(context, bulbS, moodS);
		}
	}
}

package com.kuxhausen.huemore.state;

import android.content.Context;
import android.database.Cursor;

import com.kuxhausen.huemore.persistence.Definitions.DeprecatedGroupColumns;
import com.kuxhausen.huemore.persistence.Definitions.NetBulbColumns;

import java.util.ArrayList;
import java.util.List;

public class Group {

  private String mName;
  private List<Long> mNetworkBulbDatabaseIds;

  public String getName() {
    return mName;
  }

  public List<Long> getNetworkBulbDatabaseIds() {
    return mNetworkBulbDatabaseIds;
  }

  public Group(List<Long> netBulbBaseIds, String name) {
    mNetworkBulbDatabaseIds = netBulbBaseIds;
    mName = name;
  }

  public static Group loadFromDatabase(String name, Context c) {

    String[] groupColumns = {DeprecatedGroupColumns.BULB_DATABASE_ID};
    String[] gWhereClause = {name};
    Cursor cursor =
        c.getContentResolver().query(DeprecatedGroupColumns.GROUPBULBS_URI, groupColumns,
                                     DeprecatedGroupColumns.GROUP + "=?", gWhereClause, null);

    ArrayList<Long> netBulbDbIds = new ArrayList<Long>();
    while (cursor.moveToNext()) {
      netBulbDbIds.add(cursor.getLong(0));
    }
    cursor.close();

    Group result = new Group(netBulbDbIds, name);
    return result;
  }

  public static Group loadFromLegacyData(Integer[] bulbs, String groupName, Context c) {
    ArrayList<Long> netBulbDbIds = new ArrayList<Long>();

    String[] projections = {NetBulbColumns._ID};
    for (Integer deviceId : bulbs) {
      String[] selectionArgs =
          {"" + deviceId, "" + NetBulbColumns.NetBulbType.PHILIPS_HUE};

      Cursor cursor =
          c.getContentResolver().query(
              NetBulbColumns.URI,
              projections,
              NetBulbColumns.DEVICE_ID_COLUMN + " =? AND "
              + NetBulbColumns.TYPE_COLUMN + " =?", selectionArgs, null
          );

      if (cursor.moveToFirst()) {
        netBulbDbIds.add(cursor.getLong(0));
      }
      cursor.close();
    }

    Group result = new Group(netBulbDbIds, groupName);
    return result;
  }

  public boolean conflictsWith(Group other) {
    for (Long mBulbId : mNetworkBulbDatabaseIds) {
      for (Long oBulbId : other.mNetworkBulbDatabaseIds) {
        if (mBulbId.equals(oBulbId)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object g) {
    if (g instanceof Group) {
      if (this.getNetworkBulbDatabaseIds().equals(((Group) g).getNetworkBulbDatabaseIds())) {
        //can't use this.mName.equals(((Group) g).getName())) { because NFC
        return true;
      }
    }
    return false;
  }
}

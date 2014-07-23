package com.kuxhausen.huemore.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kuxhausen.huemore.persistence.Definitions.MoodColumns;
import com.kuxhausen.huemore.persistence.Definitions.PreferenceKeys;
import com.kuxhausen.huemore.state.BulbState;
import com.kuxhausen.huemore.state.Event;
import com.kuxhausen.huemore.state.Mood;

public class Utils {

  public static Mood getMoodFromDatabase(String moodName, Context ctx) {
    String[] moodColumns = {MoodColumns.COL_MOOD_VALUE};
    String[] mWhereClause = {moodName};
    Cursor moodCursor =
        ctx.getContentResolver().query(Definitions.MoodColumns.MOODS_URI, moodColumns,
                                       MoodColumns.COL_MOOD_NAME + "=?", mWhereClause, null);
    moodCursor.moveToFirst();
    try {
      return HueUrlEncoder.decode(moodCursor.getString(0)).second.first;
    } catch (InvalidEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    } catch (FutureEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  public static Mood generateSimpleMood(BulbState bs) {
    // boilerplate
    Event e = new Event();
    e.channel = 0;
    e.time = 0;
    e.state = bs;
    Event[] eRay = {e};
    // more boilerplate
    Mood m = new Mood();
    m.usesTiming = false;
    m.events = eRay;

    return m;
  }

  public static boolean hasProVersion(Context c) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
    return settings.getInt(PreferenceKeys.BULBS_UNLOCKED, 0) > PreferenceKeys.ALWAYS_FREE_BULBS;
  }

  /**
   * Inspired by https://github.com/PhilipsHue/PhilipsHueSDK-iOS-OSX/blob/master/ApplicationDesignNotes
   * /RGB%20to%20xy%20Color%20conversion.md
   *
   * @param h in 0 to 1 in the wide RGB D65 space
   * @param s in 0 to 1 in the wide RGB D65 space
   * @return CIE 1931 xy each ranging 0 to 1
   */
  public static Float[] hsTOxy(Float[] input) {

    float h = Float.valueOf(input[0]);
    float s = Float.valueOf(input[1]);

    h = Math.max(0f, Math.min(h, 1f));
    s = Math.max(0f, Math.min(s, 1f));

    float[] hsv = {h * 360, s, 1};
    int rgb = Color.HSVToColor(hsv);

    float red = ((rgb >>> 16) & 0xFF) / 255f;
    float green = ((rgb >>> 8) & 0xFF) / 255f;
    float blue = ((rgb) & 0xFF) / 255f;

    red =
        (float) ((red > 0.04045f) ? Math.pow((red + 0.055f) / (1.0f + 0.055f), 2.4f)
                                  : (red / 12.92f));
    green =
        (float) ((green > 0.04045f) ? Math.pow((green + 0.055f) / (1.0f + 0.055f), 2.4f)
                                    : (green / 12.92f));
    blue =
        (float) ((blue > 0.04045f) ? Math.pow((blue + 0.055f) / (1.0f + 0.055f), 2.4f)
                                   : (blue / 12.92f));
    float X = red * 0.649926f + green * 0.103455f + blue * 0.197109f;
    float Y = red * 0.234327f + green * 0.743075f + blue * 0.022598f;
    float Z = red * 0.0000000f + green * 0.053077f + blue * 1.035763f;
    float x = X / (X + Y + Z);
    float y = Y / (X + Y + Z);

    Float[] result = {x, y};

    // Log.e("colorspace", "h"+h+" s"+s+" to x"+x+" y"+y);
    return result;
  }

  /**
   * Inspired by https://github.com/PhilipsHue/PhilipsHueSDK-iOS-OSX/blob/master/ApplicationDesignNotes
   * /RGB%20to%20xy%20Color%20conversion.md
   *
   * @param x CIE 1931 x ranging from 0 to 1
   * @param y CIE 1931 y ranging from 0 to 1
   * @return h, s each ranging 0 to 1 in the wide RGB D65 space
   */
  public static Float[] xyTOhs(Float[] input) {
    float x = Float.valueOf(input[0]);
    float y = Float.valueOf(input[1]);

    float z = 1.0f - x - y;
    float Y = 1f; // The given brightness value
    float X = (Y / y) * x;
    float Z = (Y / y) * z;
    float r = X * 1.612f - Y * 0.203f - Z * 0.302f;
    float g = -X * 0.509f + Y * 1.412f + Z * 0.066f;
    float b = X * 0.026f - Y * 0.072f + Z * 0.962f;
    r =
        (float) (r <= 0.0031308f ? 12.92f * r : (1.0f + 0.055f) * Math.pow(r, (1.0f / 2.4f))
                                                - 0.055f);
    g =
        (float) (g <= 0.0031308f ? 12.92f * g : (1.0f + 0.055f) * Math.pow(g, (1.0f / 2.4f))
                                                - 0.055f);
    b =
        (float) (b <= 0.0031308f ? 12.92f * b : (1.0f + 0.055f) * Math.pow(b, (1.0f / 2.4f))
                                                - 0.055f);

    float max = Math.max(r, Math.max(g, b));
    r = r / max;
    g = g / max;
    b = b / max;
    r = Math.max(r, 0);
    g = Math.max(g, 0);
    b = Math.max(b, 0);

    float[] hsv = new float[3];
    Color.RGBToHSV((int) (r * 0xFF), (int) (g * 0xFF), (int) (b * 0xFF), hsv);

    float h = hsv[0] / 360;
    float s = hsv[1];

    h = Math.max(0f, Math.min(h, 1f));
    s = Math.max(0f, Math.min(s, 1f));

    Float[] result = {h, s};
    // Log.e("colorspace", "h"+h+" s"+s+" from x"+x+" y"+y);
    return result;
  }


  /**
   * @param x CIE 1931 x ranging from 0 to 1
   * @param y CIE 1931 y ranging from 0 to 1
   * @return h, s each ranging 0 to 1 in the sRGB D65 space
   */
  public static Float[] xyTOsRGBhs(Float[] input) {
    float x = Float.valueOf(input[0]);
    float y = Float.valueOf(input[1]);

    float z = 1.0f - x - y;
    float Y = 1f; // The given brightness value
    float X = (Y / y) * x;
    float Z = (Y / y) * z;
    float r = X * 3.2404542f + Y * -1.5371385f + Z * -0.4985314f;
    float g = X * -0.9692660f + Y * 1.8760108f + Z * 0.0415560f;
    float b = X * 0.0556434f + Y * -0.2040259f + Z * 1.0572252f;
    r =
        (float) (r <= 0.0031308f ? 12.92f * r : (1.0f + 0.055f) * Math.pow(r, (1.0f / 2.4f))
                                                - 0.055f);
    g =
        (float) (g <= 0.0031308f ? 12.92f * g : (1.0f + 0.055f) * Math.pow(g, (1.0f / 2.4f))
                                                - 0.055f);
    b =
        (float) (b <= 0.0031308f ? 12.92f * b : (1.0f + 0.055f) * Math.pow(b, (1.0f / 2.4f))
                                                - 0.055f);

    float max = Math.max(r, Math.max(g, b));
    r = r / max;
    g = g / max;
    b = b / max;
    r = Math.max(r, 0);
    g = Math.max(g, 0);
    b = Math.max(b, 0);

    float[] hsv = new float[3];
    Color.RGBToHSV((int) (r * 0xFF), (int) (g * 0xFF), (int) (b * 0xFF), hsv);

    float h = hsv[0] / 360;
    float s = hsv[1];

    h = Math.max(0f, Math.min(h, 1f));
    s = Math.max(0f, Math.min(s, 1f));

    Float[] result = {h, s};
    // Log.e("colorspace", "h"+h+" s"+s+" from x"+x+" y"+y);
    return result;
  }

  /**
   * Plankian locus cubic spline approximation by Kim et al inspired by
   * http://en.wikipedia.org/wiki/Planckian_locus
   *
   * @param ct Planckian locus color temperature in Mirads
   * @return x, y in CIE 1931 each ranging 0 to 1
   */
  public static Float[] ctTOxy(float ctMirads) {
    double ct = 1000000 / ctMirads;
    double xc = 0;
    double yc = 0;

    if (1667 <= ct && ct <= 4000) {
      xc =
          -0.2661239 * (Math.pow(10, 9) / Math.pow(ct, 3)) - 0.2343580
                                                             * (Math.pow(10, 6) / Math.pow(ct, 2))
          + 0.8776956 * (Math.pow(10, 3) / ct)
          + 0.179910d;
    } else if (4000 < ct && ct < 25000) {
      xc =
          -3.0258469 * (Math.pow(10, 9) / Math.pow(ct, 3)) + 2.1070379
                                                             * (Math.pow(10, 6) / Math.pow(ct, 2))
          + 0.2226347 * (Math.pow(10, 3) / ct)
          + 0.240390d;
    }
    if (1667 <= ct && ct <= 2222) {
      yc =
          -1.1063814 * Math.pow(xc, 3) - 1.34811020 * Math.pow(xc, 2) + 2.18555832 * xc
          - 0.20219683d;
    } else if (2222 < ct && ct <= 4000) {
      yc =
          -0.9549476 * Math.pow(xc, 3) - 1.37418593 * Math.pow(xc, 2) + 2.09137015 * xc
          - 0.16748867d;
    } else if (4000 < ct && ct <= 25000) {
      yc =
          3.0817580 * Math.pow(xc, 3) - 5.87338670 * Math.pow(xc, 2) + 3.75112997 * xc
          - 0.37001483d;
    }
    Log.e("ctConversion", ct + " , " + xc + " , " + yc);
    Float[] result = {(float) xc, (float) yc};
    return result;
  }

}
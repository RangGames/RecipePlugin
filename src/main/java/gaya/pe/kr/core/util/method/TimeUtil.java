package gaya.pe.kr.core.util.method;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초");

    public static long getTimeDiffTwoDay(Date before, Date after) {
        return (after.getTime() - before.getTime()) / 1000L / 60L / 60L / 24L;
    }

    public static long getTimeDiffDay(Date date) {
        Date now = new Date();
        return (now.getTime() - date.getTime()) / 1000L / 60L / 60L / 24L;
    }

    public static long getTimeDiffHour(Date date) {
        Date now = new Date();
        return (now.getTime() - date.getTime()) / 1000L / 60L / 60L;
    }

    public static long getTimeDiffMinute(Date date) {
        Date now = new Date();
        return (now.getTime() - date.getTime()) / 1000L / 60L;
    }

    public static long getTimeDiffSec(Date date) {
        Date now = new Date();
        return (now.getTime() - date.getTime()) / 1000L;
    }

    public static String getTimeMinSec(int time) {
        if (time >= 60) {
            int min = time / 60;
            int sec = time % 60;
            if (sec != 0) {
                return String.format("%d분 %d초", min, sec);
            }
            return String.format("%d분", min);
        }
        return String.format("%d초", time);
    }

    public static Date getAfterMinTime(int min) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(12, min);
        return calendar.getTime();
    }

    public static Date getModifyDayTime(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, day);
        return calendar.getTime();
    }

    public static String getDateFormat(Date date) {
        return simpleDateFormat.format(date);
    }
}


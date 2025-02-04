package util;

public class PostPostion {
    public static String postpostion(String string, Integer integer) {
        final char lastName = string.charAt(string.length() - 1);
        if (lastName < '\uac00' || lastName > '\ud7a3') {
            return "";
        }
        String firstValue = null;
        String secondValue = null;
        if (integer == 1) {
            firstValue = "\uc740";
            secondValue = "\ub294";
        }
        else if (integer == 2) {
            firstValue = "\uc774";
            secondValue = "\uac00";
        }
        else if (integer == 3) {
            firstValue = "\uc744";
            secondValue = "\ub97c";
        }
        else if (integer == 4) {
            firstValue = "\uc73c\ub85c";
            secondValue = "\ub85c";
        }
        return new String(((lastName - '\uac00') % 28 > 0) ? firstValue : secondValue);
    }
}

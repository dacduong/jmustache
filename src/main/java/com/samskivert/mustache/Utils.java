package com.samskivert.mustache;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Utils {
    public static final int ALIGN_LEFT = 0;

    public static final int ALIGN_RIGHT = 1;

    public static final int ALIGN_CENTER = 2;

    /**
     * @param source
     * @param args   {separator},f=yyyyMMdd{separator}a=0{separator}l=20
     *               f: format - Date eg: yyyyMMdd, Decimal/Number eg: #.##
     *               a: align - 0:left, 1: right, 2: center
     *               l: length
     *               if no {separator} param, default is &
     * @return formatted st
     */
    public static String format(Object source, String args) {
        int len = 0;
        int alignment = 0;
        String token = " ";
        String separator = "&";
        String format = null;
        int pos = args.indexOf(",");
        if (pos > 0) {
            String[] arr = args.split(",");
            separator = arr[0].substring(0, 1);
            args = args.substring(pos + 1);
        }
        String[] argsArr = args.split(separator);
        for (String arg : argsArr) {
            String[] keyValue = arg.split("=");
            switch (keyValue[0]) {
                case "a":
                    alignment = Integer.parseInt(keyValue[1]);
                    break;
                case "f":
                    format = keyValue[1];
                    break;
                case "l":
                    len = Integer.parseInt(keyValue[1]);
                    break;
                case "t":
                    token = keyValue[1];
                    break;
                default:
                    break;
            }
        }

        String st = source.toString();
        if (format != null) {
            if (source instanceof Date) {
                SimpleDateFormat fmt = new SimpleDateFormat(format);
                st = fmt.format((Date) source);
            } else if (source instanceof Number) {
                DecimalFormat df = new DecimalFormat(format);
                st = df.format(source);
            } else if (source instanceof LocalDateTime) {
                DateTimeFormatter dft = DateTimeFormatter.ofPattern(format);
                st = dft.format((LocalDateTime) source);
            } else if (source instanceof LocalDate) {
                DateTimeFormatter dft = DateTimeFormatter.ofPattern(format);
                st = dft.format((LocalDate) source);
            } else if (source instanceof ZonedDateTime) {
                DateTimeFormatter dft = DateTimeFormatter.ofPattern(format);
                st = dft.format((ZonedDateTime) source);
            } else if (source instanceof LocalTime) {
                DateTimeFormatter dft = DateTimeFormatter.ofPattern(format);
                st = dft.format((LocalTime) source);
            }
        }

        if (alignment == ALIGN_LEFT) {
            return StringUtils.rightPad(StringUtils.truncate(st, len), len, token);
        } else if (alignment == ALIGN_RIGHT) {
            return StringUtils.leftPad(StringUtils.truncate(st, len), len, token);
        } else if (alignment == ALIGN_CENTER) {
            String truncatedStr = StringUtils.truncate(st, len);
            int diff = len - truncatedStr.length();
            if (diff % 2 == 0) {
                return StringUtils.rightPad(StringUtils.leftPad(truncatedStr, len - (diff / 2), token), len, token);
            } else {
                return StringUtils.rightPad(StringUtils.leftPad(truncatedStr, len - ((diff + 1) / 2), token),
                                            len,
                                            token);
            }
        }
        return StringUtils.truncate(st, len);
    }
}

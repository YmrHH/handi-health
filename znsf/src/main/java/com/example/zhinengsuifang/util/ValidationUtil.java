package com.example.zhinengsuifang.util;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static String normalizeCnMobile(String phone) {
        if (phone == null) {
            return null;
        }
        String raw = phone.trim();
        if (raw.isEmpty()) {
            return raw;
        }

        StringBuilder digits = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }

        String d = digits.toString();
        if (d.startsWith("0086")) {
            d = d.substring(4);
        } else if (d.startsWith("86") && d.length() > 11) {
            d = d.substring(2);
        }
        return d;
    }

    public static boolean isValidCnMobile(String phone) {
        String p = normalizeCnMobile(phone);
        if (p == null) {
            return false;
        }
        if (p.length() != 11) {
            return false;
        }
        if (p.charAt(0) != '1') {
            return false;
        }
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static String normalizeIdCard(String idCard) {
        if (idCard == null) {
            return null;
        }
        String s = idCard.trim();
        if (s.isEmpty()) {
            return s;
        }
        if (s.length() == 18) {
            return s.substring(0, 17) + Character.toUpperCase(s.charAt(17));
        }
        return s;
    }

    public static boolean isValidChineseIdCard18(String idCard) {
        if (idCard == null) {
            return false;
        }
        String s = normalizeIdCard(idCard);
        if (s.length() != 18) {
            return false;
        }

        for (int i = 0; i < 17; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        char last = s.charAt(17);
        if (!((last >= '0' && last <= '9') || last == 'X')) {
            return false;
        }
        return true;
    }
}

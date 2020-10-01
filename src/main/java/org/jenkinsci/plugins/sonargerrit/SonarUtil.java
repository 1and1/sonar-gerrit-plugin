package org.jenkinsci.plugins.sonargerrit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SonarUtil {
    static Pattern pattern = Pattern.compile(".*\\((?<key>.*)\\)");

    public static String isolateComponentKey(String value) {
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            return matcher.group("key");
        } else {
            return value;
        }
    }
}

package org.vaadin.leif.stylenamefinder.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.FastStringSet;

public class StyleNameMatch implements Comparable<StyleNameMatch> {

    private static final RegExp spaceSplitter = RegExp.compile("\\s+");
    private static final RegExp styleNameSplitter = RegExp.compile("\\.");

    // (tag)?.(styleNames)
    private static final RegExp segmentChecker = RegExp
            .compile("^([^.]*)\\.([^:\\[]+)$");

    private final String styleName;
    private double match;

    public StyleNameMatch(String styleName, double match) {
        this.styleName = styleName;
        this.match = match;
    }

    public static List<StyleNameMatch> orderStyleNames(JsArrayString selectors,
            Widget widget) {

        Map<String, Double> matches = findStyleNames(selectors, widget);

        ArrayList<StyleNameMatch> list = new ArrayList<StyleNameMatch>();
        for (Entry<String, Double> entry : matches.entrySet()) {
            list.add(new StyleNameMatch(entry.getKey(), entry.getValue()
                    .doubleValue()));
        }

        Collections.sort(list);

        return list;
    }

    private static Map<String, Double> findStyleNames(JsArrayString selectors,
            Widget widget) {
        Map<String, Double> matches = new HashMap<String, Double>();

        String primaryStyleName = widget.getStylePrimaryName();
        String primaryPrefix = primaryStyleName + "-";

        JsArrayString widgetStyleNames = JavaScriptObject.createArray().cast();

        SplitResult widgetStyleSplit = spaceSplitter.split(widget
                .getStyleName());
        for (int i = 0; i < widgetStyleSplit.length(); i++) {
            widgetStyleNames.push(widgetStyleSplit.get(i));
        }

        for (int i = 0; i < selectors.length(); i++) {
            String selector = selectors.get(i);

            SplitResult splitResult = spaceSplitter.split(selector);
            for (int j = 0; j < splitResult.length(); j++) {
                String segment = splitResult.get(j);

                if (!segment.contains(primaryStyleName)) {
                    continue;
                }

                MatchResult result = segmentChecker.exec(segment);
                if (result == null) {
                    continue;
                }

                String tagName = result.getGroup(1);
                if (tagName != null && !tagName.isEmpty()
                        && !tagName.equals(widget.getElement())) {
                    continue;
                }

                // Collect all style names from the selector
                FastStringSet stylenames = FastStringSet.create();

                SplitResult styleNameSplit = styleNameSplitter.split(result
                        .getGroup(2));
                for (int k = 0; k < styleNameSplit.length(); k++) {
                    stylenames.add(styleNameSplit.get(k));
                }

                boolean containsPrimary = stylenames.contains(primaryStyleName);

                // Remove all style names present on the widget
                for (int k = 0; k < widgetStyleNames.length(); k++) {
                    stylenames.remove(widgetStyleNames.get(k));
                }

                JsArrayString dump = stylenames.dump();

                // Only one style name missing?
                if (dump.length() == 1) {
                    double match = 0;
                    String styleName = dump.get(0);
                    if (styleName.startsWith(primaryPrefix)) {
                        styleName = styleName.substring(primaryPrefix.length());
                        match = 1;
                    } else if (containsPrimary) {
                        match = 0.5;
                    }

                    if (match > 0) {
                        Double oldValue = matches.get(styleName);
                        if (oldValue == null || oldValue.doubleValue() < match) {
                            matches.put(styleName, Double.valueOf(match));
                        }
                    }
                }
            }
        }
        return matches;
    }

    public String getStyleName() {
        return styleName;
    }

    public double getMatch() {
        return match;
    }

    @Override
    public int compareTo(StyleNameMatch o) {
        if (o == this) {
            return 0;
        }

        int result = Double.compare(match, o.match);
        if (result != 0) {
            return -result;
        } else {
            return styleName.compareTo(o.styleName);
        }
    }

    @Override
    public String toString() {
        return styleName + ": " + match;
    }
}

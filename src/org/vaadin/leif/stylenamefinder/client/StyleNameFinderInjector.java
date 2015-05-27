package org.vaadin.leif.stylenamefinder.client;

import com.google.gwt.core.client.EntryPoint;
import com.vaadin.client.debug.internal.VDebugWindow;

public class StyleNameFinderInjector implements EntryPoint {
    @Override
    public void onModuleLoad() {
        VDebugWindow debugWindow = VDebugWindow.get();
        if (debugWindow != null && debugWindow.isAttached()) {
            // Not attached if get() returns dummy instance
            debugWindow.addSection(new StyleNameFinderSection());
        }
    }
}

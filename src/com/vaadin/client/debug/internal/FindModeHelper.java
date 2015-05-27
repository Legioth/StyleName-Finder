package com.vaadin.client.debug.internal;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.Util;
import com.vaadin.client.WidgetUtil;

// Adapted from similar non-reusable code in HierarchySection
// Located in internal package to get access to the Highlight class
public abstract class FindModeHelper {
    private HandlerRegistration highlightModeRegistration = null;
    private Button find;

    public FindModeHelper(Button findButton) {
        find = findButton;
        find.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                startFind();
            }
        });
    }

    private boolean isFindMode() {
        return (highlightModeRegistration != null);
    }

    protected abstract void onFind(ComponentConnector connector, boolean clicked);

    public void startFind() {
        Highlight.hideAll();
        if (!isFindMode()) {
            highlightModeRegistration = Event
                    .addNativePreviewHandler(highlightModeHandler);
            find.addStyleDependentName(VDebugWindow.STYLENAME_ACTIVE);
        }
    }

    public void stopFind() {
        if (isFindMode()) {
            highlightModeRegistration.removeHandler();
            highlightModeRegistration = null;
            find.removeStyleDependentName(VDebugWindow.STYLENAME_ACTIVE);
        }
    }

    private final NativePreviewHandler highlightModeHandler = new NativePreviewHandler() {

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {

            if (event.getTypeInt() == Event.ONKEYDOWN
                    && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                stopFind();
                Highlight.hideAll();
                return;
            }
            if (event.getTypeInt() == Event.ONMOUSEMOVE) {
                Highlight.hideAll();
                Element eventTarget = WidgetUtil.getElementFromPoint(event
                        .getNativeEvent().getClientX(), event.getNativeEvent()
                        .getClientY());
                if (VDebugWindow.get().getElement().isOrHasChild(eventTarget)) {
                    return;
                }

                for (ApplicationConnection a : ApplicationConfiguration
                        .getRunningApplications()) {
                    ComponentConnector connector = Util.getConnectorForElement(
                            a, a.getUIConnector().getWidget(), eventTarget);
                    if (connector == null) {
                        connector = Util.getConnectorForElement(a,
                                RootPanel.get(), eventTarget);
                    }
                    if (connector != null) {
                        Highlight.show(connector);
                        onFind(connector, false);
                        event.cancel();
                        event.consume();
                        event.getNativeEvent().stopPropagation();
                        return;
                    }
                }
            }
            if (event.getTypeInt() == Event.ONCLICK) {
                Highlight.hideAll();
                event.cancel();
                event.consume();
                event.getNativeEvent().stopPropagation();
                stopFind();
                Element eventTarget = WidgetUtil.getElementFromPoint(event
                        .getNativeEvent().getClientX(), event.getNativeEvent()
                        .getClientY());
                for (ApplicationConnection a : ApplicationConfiguration
                        .getRunningApplications()) {
                    ComponentConnector connector = Util.getConnectorForElement(
                            a, a.getUIConnector().getWidget(), eventTarget);
                    if (connector == null) {
                        connector = Util.getConnectorForElement(a,
                                RootPanel.get(), eventTarget);
                    }

                    if (connector != null) {
                        onFind(connector, true);
                        return;
                    }
                }
            }
            event.cancel();
        }

    };
}

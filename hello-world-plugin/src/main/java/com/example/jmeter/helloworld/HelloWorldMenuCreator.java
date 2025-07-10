package com.example.jmeter.helloworld;

import org.apache.jmeter.gui.plugin.MenuCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Registers the {@link HelloWorldMenuItem} under the RUN menu so users can
 * toggle the side panel, mirroring the pattern used by the Feather Wand plug-in.
 */
public class HelloWorldMenuCreator implements MenuCreator {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldMenuCreator.class);

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if (location == MENU_LOCATION.RUN) {
            try {
                JMenu parent = new JMenu("HelloWorld");
                return new JMenuItem[]{new HelloWorldMenuItem(parent)};
            } catch (Exception ex) {
                log.error("Unable to create Hello World menu item", ex);
            }
        }
        return new JMenuItem[0];
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menu) {
        return false;
    }

    @Override
    public void localeChanged() {
        // nothing to do
    }
} 
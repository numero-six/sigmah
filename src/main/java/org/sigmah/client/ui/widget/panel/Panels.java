package org.sigmah.client.ui.widget.panel;

/*
 * #%L
 * Sigmah
 * %%
 * Copyright (C) 2010 - 2016 URD
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.sigmah.client.util.ClientUtils;

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Layout;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.user.client.Element;

/**
 * Panel utility class providing utility methods for panels.
 * 
 * @author Denis Colliot (dcolliot@ideia.fr)
 */
public final class Panels {

	// --
	// Content panel.
	// --

	/**
	 * Builds a new {@link ContentPanel} with default {@link FitLayout} that is not collapsible.
	 * 
	 * @param title
	 *          The panel header title (html is supported).
	 *          If {@code null}, header is disabled and automatically hidden.
	 * @param stylenames
	 *          (optional) Style names added to the panel component.
	 *          {@code null} values are ignored.
	 * @return The {@link ContentPanel} instance.
	 * @see ContentPanel
	 */
	public static ContentPanel content(final String title, final String... stylenames) {

		return content(title, false, null, stylenames);

	}

	/**
	 * Builds a new {@link ContentPanel} with default {@link FitLayout}.
	 * 
	 * @param title
	 *          The panel header title (html is supported).
	 *          If {@code null}, header is disabled and automatically hidden.
	 * @param collapsible
	 *          {@code true} to set the panel collapsible (expand/collapse toggle button).
	 * @param stylenames
	 *          (optional) Style names added to the panel component.
	 *          {@code null} values are ignored.
	 * @return The {@link ContentPanel} instance.
	 * @see ContentPanel
	 */
	public static ContentPanel content(final String title, final boolean collapsible, final String... stylenames) {

		return content(title, collapsible, null, stylenames);

	}

	/**
	 * Builds a new {@link ContentPanel} with default {@link FitLayout}.
	 * 
	 * @param title
	 *          The panel header title (html is supported).
	 *          If {@code null}, header is disabled and automatically hidden.
	 * @param scroll
	 *          The {@link Scroll} configuration. If {@code null}, no scroll is set.
	 * @param stylenames
	 *          (optional) Style names added to the panel component.
	 *          {@code null} values are ignored.
	 * @return The {@link ContentPanel} instance.
	 * @see ContentPanel
	 */
	public static ContentPanel content(final String title, final Scroll scroll, final String... stylenames) {

		return content(title, false, null, scroll, stylenames);

	}

	/**
	 * Builds a new {@link ContentPanel} with the given {@code layout}.
	 * 
	 * @param title
	 *          The panel header title (html is supported).
	 *          If {@code null}, header is disabled and automatically hidden.
	 * @param layout
	 *          The panel layout. If {@code null}, default {@link FitLayout} is set.
	 * @param stylenames
	 *          (optional) Style names added to the panel component.
	 *          {@code null} values are ignored.
	 * @return The {@link ContentPanel} instance.
	 * @see ContentPanel
	 */
	public static ContentPanel content(final String title, final Layout layout, final String... stylenames) {

		return content(title, false, layout, stylenames);

	}

	/**
	 * Builds a new {@link ContentPanel}.
	 * 
	 * @param title
	 *          The panel header title (html is supported).
	 *          If {@code null}, header is disabled and automatically hidden.
	 * @param collapsible
	 *          {@code true} to set the panel collapsible (expand/collapse toggle button).
	 * @param layout
	 *          The panel layout. If {@code null}, default {@link FitLayout} is set.
	 * @param stylenames
	 *          (optional) Style names added to the panel component.
	 *          {@code null} values are ignored.
	 * @return The {@link ContentPanel} instance.
	 * @see ContentPanel
	 */
	public static ContentPanel content(final String title, final boolean collapsible, final Layout layout, final String... stylenames) {

		return content(title, collapsible, layout, null, stylenames);

	}

	/**
	 * Builds a new {@link ContentPanel}.
	 * 
	 * @param title
	 *          The panel header title (html is supported).
	 *          If {@code null}, header is disabled and automatically hidden.
	 * @param collapsible
	 *          {@code true} to set the panel collapsible (expand/collapse toggle button).
	 * @param layout
	 *          The panel layout. If {@code null}, default {@link FitLayout} is set.
	 * @param scroll
	 *          The {@link Scroll} configuration. If {@code null}, no scroll is set.
	 * @param stylenames
	 *          (optional) Style names added to the panel component.
	 *          {@code null} values are ignored.
	 * @return The {@link ContentPanel} instance.
	 * @see ContentPanel
	 */
	public static ContentPanel content(final String title, final boolean collapsible, final Layout layout, final Scroll scroll, final String... stylenames) {

		final ContentPanel panel = new ContentPanel(layout != null ? layout : new FitLayout());

		panel.setHeadingHtml(ClientUtils.isNotBlank(title) ? title : null);
		panel.setHeaderVisible(ClientUtils.isNotBlank(title));
		panel.setCollapsible(collapsible);

		if (ClientUtils.isNotEmpty(stylenames)) {
			for (final String stylename : stylenames) {
				if (ClientUtils.isBlank(stylename)) {
					continue;
				}
				panel.addStyleName(stylename);
			}
		}

		if (scroll != null) {
			panel.setScrollMode(scroll);
		}

		return panel;
	}

	// --
	// Tab panel.
	// --

	/**
	 * Builds a new {@link TabPanel} with default transparent background.
	 * 
	 * @param stylenames
	 *          (optional) Style names added to the panel content component.
	 *          {@code null} values are ignored.
	 * @return The {@link TabPanel} instance.
	 * @see TabPanel
	 */
	public static TabPanel tab(final String... stylenames) {

		final TabPanel panel = new TabPanel() {

			@Override
			protected void onRender(final Element target, final int index) {
				super.onRender(target, index);

				if (ClientUtils.isNotEmpty(stylenames)) {
					for (final String stylename : stylenames) {
						if (ClientUtils.isBlank(stylename)) {
							continue;
						}
						getLayoutTarget().addStyleName(stylename);
					}
				}
			}
		};
		
		panel.addStyleName("sigmah-tab-panel");

		panel.setBorderStyle(false);
		panel.setBodyBorder(false);

		return panel;
	}

	/**
	 * Private constructor.
	 */
	private Panels() {
		// Factory pattern.
	}

}

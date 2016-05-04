package org.sigmah.client.ui.widget;

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


import com.extjs.gxt.ui.client.core.El;
import java.util.Arrays;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.EditorGrid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;

/**
 * A basic GXT grid with some extra features.
 * 
 * @param <M>
 *          Type of data stored in this grid.
 * @author tmi
 * @author Denis Colliot (dcolliot@ideia.fr)
 */
public class FlexibleGrid<M extends ModelData> extends EditorGrid<M> {

	/**
	 * The number of elements to display (before adding a scrollbar).
	 * A negative or <code>null</code> value displays all elements by default.
	 */
	private int visibleElementsCount = -1;

	/**
	 * Creates a new grid.
	 * 
	 * @param store
	 *          The data store.
	 * @param selectionModel
	 *          If the grid must implements a default checkbox selection model.
	 * @param columns
	 *          The columns model configurations.
	 */
	public FlexibleGrid(ListStore<M> store, GridSelectionModel<M> selectionModel, ColumnConfig... columns) {
		this(store, selectionModel, -1, columns);
	}

	/**
	 * Creates a new grid.
	 * 
	 * @param store
	 *          The data store.
	 * @param selectionModel
	 *          If the grid must implements a default checkbox selection model.
	 * @param visibleElementsCount
	 *          The number of elements displayed.
	 * @param columns
	 *          The columns model configurations.
	 */
	public FlexibleGrid(ListStore<M> store, GridSelectionModel<M> selectionModel, int visibleElementsCount, ColumnConfig... columns) {
		super(store, new ColumnModel(Arrays.asList(columns)));

		this.visibleElementsCount = visibleElementsCount;

		// Some default values.
		this.getView().setForceFit(true);
		setBorders(false);

		if (selectionModel != null) {
			setSelectionModel(selectionModel);
			if (selectionModel instanceof ComponentPlugin) {
				addPlugin((ComponentPlugin) selectionModel);
			}
		}

		// Manages the grid's height adjustments.
		this.addListener(Events.ViewReady, new Listener<ComponentEvent>() {

			@Override
			public void handleEvent(ComponentEvent be) {
				getStore().addListener(Store.Add, new Listener<StoreEvent<M>>() {

					@Override
					public void handleEvent(StoreEvent<M> be) {
						doAutoHeight();
					}
				});
				getStore().addListener(Store.Remove, new Listener<StoreEvent<M>>() {

					@Override
					public void handleEvent(StoreEvent<M> be) {
						doAutoHeight();
					}
				});
				getStore().addListener(Store.Clear, new Listener<StoreEvent<M>>() {

					@Override
					public void handleEvent(StoreEvent<M> be) {
						doAutoHeight();
					}
				});
				getStore().addListener(Store.Filter, new Listener<StoreEvent<M>>() {

					@Override
					public void handleEvent(StoreEvent<M> be) {
						doAutoHeight();
					}
				});
				doAutoHeight();
			}
		});
	}

	/**
	 * Gets the number of elements displayed.
	 * 
	 * @return The number of elements displayed.
	 */
	public int getVisibleElementsCount() {
		return visibleElementsCount;
	}

	/**
	 * Sets the number of elements to display (before adding a scrollbar).
	 * A negative or <code>null</code> value displays all elements by default.
	 * 
	 * @param visibleElementsCount
	 *          The number of elements to display.
	 */
	public void setVisibleElementsCount(int visibleElementsCount) {
		this.visibleElementsCount = visibleElementsCount;
	}

	/**
	 * Computes and applies the grid height depending on the number of desired elements to display.
	 */
	private void doAutoHeight() {
		
		final El fileListElement = this.getView().getBody().firstChild();

		final int elementsHeight;
		if (visibleElementsCount <= 0 || fileListElement.firstChild() == null) {
			// Shows all elements.
			elementsHeight = fileListElement.getHeight();
		} else {
			// Shows only desired elements.
			elementsHeight = fileListElement.firstChild().getHeight()
						* (getStore().getCount() > visibleElementsCount ? visibleElementsCount : getStore().getCount());
		}

		this.setHeight((this.getView().getBody().isScrollableX() ? 20 : 0)
			+ this.getView().getHeader().getHeight()
			+ this.el().getFrameWidth("tb")
			+ elementsHeight);
	}
}

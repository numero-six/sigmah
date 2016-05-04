package org.sigmah.client.ui.view.project.logframe;

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

import java.util.ArrayList;
import java.util.Collection;

import org.sigmah.client.i18n.I18N;
import org.sigmah.client.ui.notif.N10N;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.DateField;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayoutData;

/**
 * A pop-up window to fill a form.
 * 
 * @author tmi (v1.3)
 * @author Denis Colliot (dcolliot@ideia.fr) (v2.0)
 */
// TODO Should be transformed into a proper presenter.
final class FormWindow {

	/**
	 * Listen the form completion.
	 * 
	 * @author tmi
	 */
	public static interface FormSubmitListener {

		/**
		 * Method called when the form is correctly filled (values can be <code>null</code> if the null input is allowed for
		 * some fields). The values are returned in the same order in which fields have been added.
		 * 
		 * @param values
		 *          The input values.
		 */
		void formSubmitted(Object... values);

	}

	/**
	 * Manages a field displayed by this window.
	 * 
	 * @author tmi
	 */
	private static final class FieldWrapper {

		/**
		 * The form field.
		 */
		private final Field<?> field;

		/**
		 * If the field allows blank value.
		 */
		private final boolean allowBlank;

		public FieldWrapper(Field<?> field, boolean allowBlank) {
			this.field = field;
			this.allowBlank = allowBlank;
		}
	}

	/**
	 * Combobox preferred height.
	 */
	private static final int FIELD_HEIGHT = 32;

	/**
	 * Listeners.
	 */
	private final ArrayList<FormSubmitListener> listeners;

	/**
	 * The pop-up window.
	 */
	private Window window;

	/**
	 * The form title.
	 */
	private Label titleLabel;

	/**
	 * The vertical panel to display fields.
	 */
	private ContentPanel fieldsPanel;

	/**
	 * List of all fields.
	 */
	private final ArrayList<FieldWrapper> fields;

	/**
	 * Initialize the window.
	 */
	public FormWindow() {
		listeners = new ArrayList<FormSubmitListener>();
		fields = new ArrayList<FieldWrapper>();
	}

	/**
	 * Adds a listener.
	 * 
	 * @param l
	 *          The new listener.
	 */
	public void addFormSubmitListener(FormSubmitListener l) {

		if (l == null) {
			return;
		}

		listeners.add(l);
	}

	/**
	 * Removes a listener.
	 * 
	 * @param l
	 *          The old listener.
	 */
	public void removeFormSubmitListener(FormSubmitListener l) {

		if (l == null) {
			return;
		}

		listeners.remove(l);
	}

	/**
	 * Informs the listeners that the form has been filled.
	 * 
	 * @param values
	 *          The input values.
	 */
	protected void fireFormSubmitted(Object... values) {
		for (final FormSubmitListener l : listeners) {
			l.formSubmitted(values);
		}
	}

	/**
	 * Builds the pop-up window.
	 */
	private void init() {

		// Build the form label.
		titleLabel = new Label();

		// Builds the submit button.
		final Button selectButton = new Button(I18N.CONSTANTS.formWindowSubmitAction());
		selectButton.addListener(Events.OnClick, new Listener<BaseEvent>() {

			@Override
			public void handleEvent(BaseEvent be) {

				final ArrayList<Object> values = new ArrayList<Object>();

				// Retrieves each value.
				for (final FieldWrapper field : fields) {

					final Object value = field.field.getValue();

					// Checks if null value is allowed for this field.
					if (!field.allowBlank && value == null) {
						N10N.warn(I18N.CONSTANTS.formWindowFieldsUnfilled(), I18N.CONSTANTS.formWindowFieldsUnfilledDetails());
						return;
					}

					values.add(value);
				}

				fireFormSubmitted(values.toArray(new Object[values.size()]));

				// Closes the window.
				window.hide();
			}
		});

		// Builds the fields panel.
		fieldsPanel = new ContentPanel();
		final VBoxLayout fieldsPanelLayout = new VBoxLayout();
		fieldsPanelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);
		fieldsPanel.setHeaderVisible(false);
		fieldsPanel.setLayout(fieldsPanelLayout);
		fieldsPanel.setBorders(false);
		fieldsPanel.setWidth("100%");

		// Builds the main panel.
		final ContentPanel mainPanel = new ContentPanel();
		final VBoxLayout layout = new VBoxLayout();
		layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);
		mainPanel.setHeaderVisible(false);
		mainPanel.setLayout(layout);
		mainPanel.setBorders(true);
		mainPanel.setWidth("100%");

		mainPanel.setTopComponent(null);
		mainPanel.add(titleLabel, new VBoxLayoutData(new Margins(4, 8, 0, 8)));
		mainPanel.add(fieldsPanel, new VBoxLayoutData(new Margins(4, 8, 0, 8)));
		mainPanel.add(selectButton, new VBoxLayoutData(new Margins(4, 8, 0, 8)));

		// Builds window.
		window = new Window();
		window.setWidth(445);
		window.setPlain(true);
		window.setModal(true);
		window.setBlinkModal(true);
		window.setLayout(new FitLayout());

		window.add(mainPanel);
	}

	/**
	 * Initialize the window and open it.
	 * 
	 * @param title
	 *          The window title.
	 * @param header
	 *          The heading label of the selection form.
	 */
	public void show(String title, String header) {

		// Lazy building.
		if (window == null) {
			init();
		}

		titleLabel.setHtml(header);

		// Open the window.
		window.setHeadingHtml(title);
		window.setHeight(100 + (FIELD_HEIGHT * (fields.size())));
		window.show();
	}

	/**
	 * Removes all the fields of this window.
	 * This method removes also the listeners.
	 */
	public void clear() {

		if (window != null) {
			fieldsPanel.removeAll();
			fields.clear();
			listeners.clear();
			window = null;
		}
	}

	/**
	 * Cleans all fields.
	 */
	public void clean() {
		for (final FieldWrapper field : fields) {
			field.field.reset();
		}
	}

	/**
	 * Adds a text field in the window.
	 * 
	 * @param fieldLabelString
	 *          The label of the text field. Can be <code>null</code>.
	 * @param allowBlank
	 *          If the field is required.
	 * @return The field.
	 */
	public TextField<String> addTextField(String fieldLabelString, boolean allowBlank) {

		// Lazy building.
		if (window == null) {
			init();
		}

		// Builds the text field.
		final TextField<String> field = new TextField<String>();
		field.setAllowBlank(allowBlank);
		field.setFieldLabel(fieldLabelString);

		fields.add(new FieldWrapper(field, allowBlank));

		addField(field, fieldLabelString);

		return field;
	}

	/**
	 * Adds a number field in the window.
	 * 
	 * @param fieldLabelString
	 *          The label of the number field. Can be <code>null</code>.
	 * @param allowBlank
	 *          If the field is required.
	 * @return The field.
	 */
	public NumberField addNumberField(String fieldLabelString, boolean allowBlank) {

		// Lazy building.
		if (window == null) {
			init();
		}

		// Builds the text field.
		final NumberField field = new NumberField();
		field.setAllowBlank(allowBlank);
		field.setFieldLabel(fieldLabelString);

		fields.add(new FieldWrapper(field, allowBlank));

		addField(field, fieldLabelString);

		return field;
	}

	/**
	 * Adds a date field in the window.
	 * 
	 * @param fieldLabelString
	 *          The label of the date field. Can be <code>null</code>.
	 * @param allowBlank
	 *          If the field is required.
	 * @return The field.
	 */
	public DateField addDateField(String fieldLabelString, boolean allowBlank) {

		// Lazy building.
		if (window == null) {
			init();
		}

		// Builds the text field.
		final DateField field = new DateField();
		field.setAllowBlank(allowBlank);
		field.setFieldLabel(fieldLabelString);

		fields.add(new FieldWrapper(field, allowBlank));

		addField(field, fieldLabelString);

		return field;
	}

	/**
	 * Adds a label field in the window.
	 * 
	 * @param fieldLabelString
	 *          The label of the date field. Can be <code>null</code>.
	 * @return The field.
	 */
	public LabelField addLabelField(String fieldLabelString) {

		// Lazy building.
		if (window == null) {
			init();
		}

		// Builds the text field.
		final LabelField field = new LabelField();
		field.setFieldLabel(fieldLabelString);

		fields.add(new FieldWrapper(field, true));

		addField(field, fieldLabelString);

		return field;
	}

	/**
	 * Adds a list of choices available in the window. The elements are shown in a combobox with uses the field
	 * <code>label</code>. Ensure that your model provides this attribute.
	 * 
	 * @param <E>
	 *          The type of selectable elements.
	 * @param fieldLabelString
	 *          The label of the list of choices. Can be <code>null</code>.
	 * @param choices
	 *          The list of choices.
	 * @param allowBlank
	 *          If the selection for this list is required.
	 * @param displayField
	 *          The underlying data field name to bind to this list. Must not be <code>null</code>.
	 * @return The field.
	 */
	public <E extends ModelData> ComboBox<E> addChoicesList(String fieldLabelString, Collection<E> choices, boolean allowBlank, String displayField) {

		// Checks if the display field is specified.
		if (displayField == null || "".equals(displayField.trim())) {
			throw new IllegalArgumentException("the display field must not be null");
		}

		// Checks if the list of choices is correct.
		if (choices == null || choices.isEmpty()) {
			throw new IllegalArgumentException("the list of choices must contain at least one choice");
		}

		// Lazy building.
		if (window == null) {
			init();
		}

		// Builds the store.
		final ListStore<E> store = new ListStore<E>();
		store.add(new ArrayList<E>(choices));

		// Builds the combobox.
		final ComboBox<E> combobox = new ComboBox<E>();
		combobox.setStore(store);
		combobox.setEditable(true);
		combobox.setAllowBlank(allowBlank);
		combobox.setEmptyText(I18N.CONSTANTS.formWindowListEmptyText());
		combobox.setLoadingText(I18N.CONSTANTS.loading());
		combobox.setFieldLabel(fieldLabelString);
		combobox.setDisplayField(displayField.trim());
		combobox.setTriggerAction(TriggerAction.ALL);

		fields.add(new FieldWrapper(combobox, allowBlank));

		addField(combobox, fieldLabelString);

		return combobox;
	}

	/**
	 * Adds a field in the form.
	 * 
	 * @param field
	 *          The field.
	 * @param fieldLabelString
	 *          The label of the field. Can be <code>null</code>.
	 */
	private void addField(Field<?> field, String fieldLabelString) {

		// Builds the field label.
		final Label fieldLabel = new Label(fieldLabelString);
		fieldLabel.setWidth("165px");
		fieldLabel.addStyleName("flexibility-element-label");

		// Builds the field panel.
		final ContentPanel fieldPanel = new ContentPanel();
		fieldPanel.setBodyBorder(false);
		fieldPanel.setHeaderVisible(false);
		fieldPanel.setLayout(new HBoxLayout());

		fieldPanel.add(fieldLabel, new HBoxLayoutData(new Margins(4, 20, 0, 0)));
		final HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 20, 0, 0));
		flex.setFlex(1);
		fieldPanel.add(field, flex);

		// Adds the field in the panel.
		fieldsPanel.setHeight(FIELD_HEIGHT * fields.size());
		fieldsPanel.add(fieldPanel, new VBoxLayoutData(new Margins(4, 0, 0, 0)));
		fieldsPanel.layout();
	}
}

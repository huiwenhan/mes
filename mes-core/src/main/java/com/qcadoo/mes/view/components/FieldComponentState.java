package com.qcadoo.mes.view.components;

import org.json.JSONException;
import org.json.JSONObject;

import com.qcadoo.mes.view.states.AbstractComponentState;

public class FieldComponentState extends AbstractComponentState {

    public static final String JSON_REQUIRED = "required";

    public static final String JSON_VALID = "valid";

    private String value;

    private boolean required;

    @Override
    protected void initializeContent(final JSONObject json) throws JSONException {
        value = json.getString(JSON_VALUE);
    }

    @Override
    protected JSONObject renderContent() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_VALUE, value);
        json.put(JSON_REQUIRED, required);
        json.put(JSON_VALID, required);
        return json;
    }

    @Override
    public final void setFieldValue(final Object value) {
        this.value = value != null ? value.toString() : null;
        requestRender();
        requestUpdateState();
    }

    @Override
    public final Object getFieldValue() {
        return value;
    }

    public final boolean isRequired() {
        return required;
    }

    public final void setRequired(final boolean required) {
        this.required = required;
    }

}

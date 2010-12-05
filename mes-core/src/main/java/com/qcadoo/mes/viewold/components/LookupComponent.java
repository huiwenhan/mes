/**
 * ********************************************************************
 * Code developed by amazing QCADOO developers team.
 * Copyright (c) Qcadoo Limited sp. z o.o. (2010)
 * ********************************************************************
 */

package com.qcadoo.mes.viewold.components;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.api.TranslationService;
import com.qcadoo.mes.api.ViewDefinitionService;
import com.qcadoo.mes.model.DataDefinition;
import com.qcadoo.mes.model.FieldDefinition;
import com.qcadoo.mes.model.search.Restrictions;
import com.qcadoo.mes.model.search.SearchCriteriaBuilder;
import com.qcadoo.mes.model.search.SearchResult;
import com.qcadoo.mes.model.types.HasManyType;
import com.qcadoo.mes.model.validators.ErrorMessage;
import com.qcadoo.mes.utils.ExpressionUtil;
import com.qcadoo.mes.view.ComponentOption;
import com.qcadoo.mes.view.ribbon.Ribbon;
import com.qcadoo.mes.view.ribbon.RibbonActionItem;
import com.qcadoo.mes.view.ribbon.RibbonGroup;
import com.qcadoo.mes.view.ribbon.RibbonActionItem.Type;
import com.qcadoo.mes.viewold.AbstractComponent;
import com.qcadoo.mes.viewold.ContainerComponent;
import com.qcadoo.mes.viewold.SelectableComponent;
import com.qcadoo.mes.viewold.ViewDefinition;
import com.qcadoo.mes.viewold.ViewValue;
import com.qcadoo.mes.viewold.components.grid.LookupData;
import com.qcadoo.mes.viewold.containers.WindowComponent;
import com.qcadoo.mes.viewold.internal.ViewDefinitionImpl;

/**
 * Represents lookup input.<br/>
 * <br/>
 * XML declaration: <br/>
 * 
 * <pre>
 *      {@code <component type="lookup" name="{identifier of component}" field="{field name of component}" source="{source of component content}">}
 * </pre>
 * 
 * XML options:
 * <ul>
 * <li>column - integer - definition of column of lookup window (can be more than one). Suboptions:
 * <ul>
 * <li>name - String - name of column</li>
 * <li>fields - String - list of column fields</li>
 * <li>expression - String - expression which describes how to display value in cell</li>
 * <li>width - integer - width of column (actual width will be calculate dynamically, this options define proportion between
 * columns widths)</li>
 * <li>link - [true|false] - true when values of this column should be links to select entity</li>
 * </ul>
 * </li>
 * <li>height - integer - height of lookup window</li>
 * <li>width - integer - width of lookup window</li>
 * <li>fullScreen - [true|false] - true when grid should expand to full screen</li>
 * <li>orderable - comma separated list of column names - list of column names of lookup window which sorting is enabled</li>
 * <li>searchable - comma separated list of column names - list of column names of lookup window which searching is enabled</li>
 * <li>paginable - [true|false] - true when grid of lookup window should have paging</li>
 * <li>expression - String - defines how to display lookup input value</li>
 * <li>fieldCode - String - name of field that should be field code</li>
 * </ul>
 */
public final class LookupComponent extends AbstractComponent<LookupData> implements SelectableComponent {

    private int width;

    private int height;

    private String expression;

    private String fieldCode;

    public LookupComponent(final String name, final ContainerComponent<?> parent, final String fieldName,
            final String dataSource, final TranslationService translationService) {
        super(name, parent, fieldName, dataSource, translationService);
    }

    @Override
    public void initializeComponent() {
        for (ComponentOption option : getRawOptions()) {
            if ("width".equals(option.getType())) {
                width = Integer.parseInt(option.getValue());
                addOption("width", width);
            } else if ("height".equals(option.getType())) {
                height = Integer.parseInt(option.getValue());
                addOption("height", height);
            } else if ("expression".equals(option.getType())) {
                expression = option.getValue();
            } else if ("fieldCode".equals(option.getType())) {
                fieldCode = option.getValue();
            }
        }
    }

    @Override
    public String getType() {
        return "lookupComponent";
    }

    @Override
    public ViewValue<LookupData> castComponentValue(final Map<String, Entity> selectedEntities, final JSONObject viewObject)
            throws JSONException {
        LookupData lookupData = new LookupData();

        JSONObject value = viewObject.getJSONObject("value");

        if (value != null) {
            if (!value.isNull("selectedEntityId")) {
                String selectedEntityId = value.getString("selectedEntityId");

                if (selectedEntityId != null && !"null".equals(selectedEntityId)) {
                    Entity selectedEntity = getDataDefinition().get(Long.parseLong(selectedEntityId));
                    selectedEntities.put(getPath(), selectedEntity);
                    lookupData.setSelectedEntityId(Long.parseLong(selectedEntityId));
                }
            }

            if (!value.isNull("selectedEntityCode")) {
                lookupData.setSelectedEntityCode(value.getString("selectedEntityCode"));
            }

            if (!value.isNull("contextEntityId")) {
                String contextEntityId = value.getString("contextEntityId");

                if (contextEntityId != null && !"null".equals(contextEntityId)) {
                    lookupData.setContextEntityId(Long.parseLong(contextEntityId));
                }
            }
        }

        return new ViewValue<LookupData>(lookupData);
    }

    @Override
    public ViewValue<LookupData> getComponentValue(final Entity entity, final Entity parentEntity,
            final Map<String, Entity> selectedEntities, final ViewValue<LookupData> viewValue, final Set<String> pathsToUpdate,
            final Locale locale) {

        LookupData lookupData = new LookupData();

        if (getSourceFieldPath() != null) {
            Entity contextEntity = selectedEntities.get(getSourceComponent().getPath());

            if (contextEntity != null) {
                lookupData.setContextEntityId(contextEntity.getId());
            }
        }

        boolean error = false;

        Entity selectedEntity = null;

        if ((getSourceFieldPath() == null || lookupData.getContextEntityId() != null) && viewValue != null
                && viewValue.getValue() != null) {
            if (viewValue.getValue().getSelectedEntityId() != null) {
                selectedEntity = getDataDefinition().get(viewValue.getValue().getSelectedEntityId());
            } else if (StringUtils.hasText(viewValue.getValue().getSelectedEntityCode())) {
                String code = viewValue.getValue().getSelectedEntityCode();

                SearchCriteriaBuilder searchCriteriaBuilder = getDataDefinition().find().restrictedWith(
                        Restrictions.eq(getDataDefinition().getField(fieldCode), code + "*"));

                if (lookupData.getContextEntityId() != null) {
                    DataDefinition gridDataDefinition = getSourceComponent().getDataDefinition();
                    HasManyType hasManyType = getHasManyType(gridDataDefinition, getSourceFieldPath());

                    searchCriteriaBuilder.restrictedWith(Restrictions.belongsTo(
                            getDataDefinition().getField(hasManyType.getJoinFieldName()), lookupData.getContextEntityId()));
                }

                SearchResult results = searchCriteriaBuilder.list();

                if (results.getTotalNumberOfEntities() == 1) {
                    selectedEntity = results.getEntities().get(0);
                } else {
                    lookupData.setSelectedEntityCode(code);
                    error = true;
                }
            }
        }

        if (parentEntity != null && selectedEntity == null && !error && pathsToUpdate.isEmpty()) {
            selectedEntity = (Entity) getFieldValue(parentEntity, getFieldPath());
        }

        if (selectedEntity != null) {
            lookupData.setSelectedEntityValue(ExpressionUtil.getValue(selectedEntity, expression, locale));
            lookupData.setSelectedEntityId(selectedEntity.getId());
            lookupData.setSelectedEntityCode(String.valueOf(selectedEntity.getField(fieldCode)));
            selectedEntities.put(getPath(), selectedEntity);
        }

        ViewValue<LookupData> newViewValue = new ViewValue<LookupData>(lookupData);

        if (error) {
            newViewValue.addErrorMessage(getTranslationService()
                    .translate("core.validate.field.error.lookupCodeNotFound", locale));
        }

        FieldDefinition fieldDefinition = getFieldDefinition();

        if (fieldDefinition.isRequired() || (entity == null && fieldDefinition.isRequiredOnCreate())) {
            newViewValue.getValue().setRequired(true);
        }

        if (fieldDefinition.isReadOnly() || (entity != null && fieldDefinition.isReadOnlyOnUpdate())) {
            newViewValue.setEnabled(false);
        }

        if (!error) {
            ErrorMessage validationError = getErrorMessage(entity, selectedEntities);

            if (validationError != null) {
                newViewValue.addErrorMessage(getTranslationService().translateErrorMessage(validationError, locale));
            } else {
                validationError = getErrorMessage(parentEntity, selectedEntities);

                if (validationError != null) {
                    newViewValue.addErrorMessage(getTranslationService().translateErrorMessage(validationError, locale));
                }
            }
        }

        return newViewValue;
    }

    private HasManyType getHasManyType(final DataDefinition dataDefinition, final String fieldPath) {
        checkState(!fieldPath.matches("\\."), "Grid doesn't support sequential path");
        FieldDefinition fieldDefinition = dataDefinition.getField(fieldPath);
        if (fieldDefinition != null && fieldDefinition.getType() instanceof HasManyType) {
            return (HasManyType) fieldDefinition.getType();
        } else {
            throw new IllegalStateException("Grid data definition cannot be found");
        }
    }

    public ViewDefinition getLookupViewDefinition(final ViewDefinitionService viewDefinitionService) {
        String viewName = getViewDefinition().getName() + ".lookup." + getPath();

        // ViewDefinition existingLookupViewDefinition = viewDefinitionService.get(getViewDefinition().getPluginIdentifier(),
        // viewName);

        // if (existingLookupViewDefinition != null) {
        // return existingLookupViewDefinition;
        // }

        ViewDefinitionImpl lookupViewDefinition = new ViewDefinitionImpl(getViewDefinition().getPluginIdentifier(), viewName);

        DataDefinition dataDefinition;
        String sourceFieldPath;

        if (getSourceComponent() != null) {
            dataDefinition = getSourceComponent().getDataDefinition();
            sourceFieldPath = getSourceFieldPath();
        } else {
            dataDefinition = getDataDefinition();
            sourceFieldPath = null;
        }

        WindowComponent windowComponent = new WindowComponent("mainWindow", dataDefinition, lookupViewDefinition,
                getTranslationService());
        windowComponent.addRawOption(new ComponentOption("fixedHeight", ImmutableMap.of("value", "true")));
        windowComponent.addRawOption(new ComponentOption("header", ImmutableMap.of("value", "false")));
        windowComponent.addRawOption(new ComponentOption("minWidth", ImmutableMap.of("value", "false")));

        GridComponent gridComponent = new GridComponent("lookupGrid", windowComponent, null, sourceFieldPath,
                getTranslationService());

        addConstantsColumnToLookupGrid(gridComponent);

        for (ComponentOption rawOption : getRawOptions()) {

            if (rawOption.getType().equals("orderable")) {
                Map<String, String> newAttributes = new HashMap<String, String>();
                newAttributes.put("value", rawOption.getValue() + ",lookupCodeVisible");
                rawOption = new ComponentOption("orderable", newAttributes);
            }

            gridComponent.addRawOption(rawOption);
        }

        gridComponent.addRawOption(new ComponentOption("isLookup", ImmutableMap.of("value", "true")));

        windowComponent.addComponent(gridComponent);

        addRibbonToLookupWindow(windowComponent);

        lookupViewDefinition.setRoot(windowComponent);

        windowComponent.initialize();

        // viewDefinitionService.save(lookupViewDefinition);

        return lookupViewDefinition;
    }

    private void addRibbonToLookupWindow(final WindowComponent windowComponent) {
        RibbonActionItem ribbonActionItem = new RibbonActionItem();
        ribbonActionItem.setName("select");
        ribbonActionItem.setIcon("acceptIcon24.png");
        ribbonActionItem.setAction("#{mainWindow.lookupGrid}.performLookupSelect; #{mainWindow}.performClose");
        ribbonActionItem.setType(Type.BIG_BUTTON);

        RibbonActionItem ribbonCancelActionItem = new RibbonActionItem();
        ribbonCancelActionItem.setName("cancel");
        ribbonCancelActionItem.setIcon("cancelIcon24.png");
        ribbonCancelActionItem.setAction("#{mainWindow}.performClose");
        ribbonCancelActionItem.setType(Type.BIG_BUTTON);

        RibbonGroup ribbonGroup = new RibbonGroup();
        ribbonGroup.setName("navigation");
        ribbonGroup.addItem(ribbonActionItem);
        ribbonGroup.addItem(ribbonCancelActionItem);

        Ribbon ribbon = new Ribbon();
        ribbon.addGroup(ribbonGroup);

        windowComponent.setRibbon(ribbon);
    }

    private void addConstantsColumnToLookupGrid(final GridComponent gridComponent) {
        Map<String, String> valueColumnOptions = new HashMap<String, String>();
        valueColumnOptions.put("name", "lookupValue");
        valueColumnOptions.put("expression", expression);
        valueColumnOptions.put("hidden", "true");
        gridComponent.addRawOption(new ComponentOption("column", valueColumnOptions));
        Map<String, String> codeColumnOptions = new HashMap<String, String>();
        codeColumnOptions.put("name", "lookupCode");
        codeColumnOptions.put("fields", fieldCode);
        codeColumnOptions.put("hidden", "true");
        gridComponent.addRawOption(new ComponentOption("column", codeColumnOptions));
        Map<String, String> codeVisibleColumnOptions = new HashMap<String, String>();
        codeVisibleColumnOptions.put("name", "lookupCodeVisible");
        codeVisibleColumnOptions.put("fields", fieldCode);
        codeVisibleColumnOptions.put("hidden", "false");
        codeVisibleColumnOptions.put("link", "true");
        gridComponent.addRawOption(new ComponentOption("column", codeVisibleColumnOptions));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long getSelectedEntityId(final ViewValue<Long> viewValue) {
        ViewValue<LookupData> value = (ViewValue<LookupData>) lookupViewValue(viewValue);
        return value.getValue().getSelectedEntityId();
    }

    @Override
    public void addComponentTranslations(final Map<String, String> translationsMap, final Locale locale) {
        String codeBase = getViewDefinition().getPluginIdentifier() + "." + getViewDefinition().getName() + "." + getPath()
                + ".label";
        List<String> messageCodes = new LinkedList<String>();
        messageCodes.add(codeBase);
        messageCodes.add(getTranslationService().getEntityFieldBaseMessageCode(getParentContainer().getDataDefinition(),
                getName())
                + ".label");
        translationsMap.put(messageCodes.get(0), getTranslationService().translate(messageCodes, locale));

        List<String> focusMessageCodes = new LinkedList<String>();
        focusMessageCodes.add(codeBase + ".focus");
        focusMessageCodes.add(getTranslationService().getEntityFieldBaseMessageCode(getParentContainer().getDataDefinition(),
                getName())
                + ".label.focus");
        translationsMap.put(focusMessageCodes.get(0), getTranslationService().translate(focusMessageCodes, locale));
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getExpression() {
        return expression;
    }

    private ErrorMessage getErrorMessage(final Entity entity, final Map<String, Entity> selectedEntities) {
        ErrorMessage message = null;

        if (getSourceComponent() != null && selectedEntities != null) {
            message = getFieldError(selectedEntities.get(getSourceComponent().getPath()), getSourceFieldPath());
        }
        if (message == null && getSourceFieldPath() != null) {
            message = getFieldError(entity, getSourceFieldPath());
        }
        if (message == null) {
            message = getFieldError(entity, getFieldPath());
        }

        return message;
    }
}

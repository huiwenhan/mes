package com.qcadoo.mes.view;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.qcadoo.mes.model.DataDefinition;
import com.qcadoo.mes.model.HookDefinition;
import com.qcadoo.mes.view.components.WindowComponentPattern;
import com.qcadoo.mes.view.components.form.FormComponentPattern;
import com.qcadoo.mes.view.internal.ViewDefinitionImpl;
import com.qcadoo.mes.view.patterns.AbstractContainerPattern;
import com.qcadoo.mes.view.patterns.AbstractPatternTest;
import com.qcadoo.mes.view.patterns.ComponentPatternMock;
import com.qcadoo.mes.view.states.ComponentStateMock;
import com.qcadoo.mes.view.states.ComponentStateMock.TestEvent;

public class ViewDefinitionTest extends AbstractPatternTest {

    @Test
    public void shouldHaveBasicInformation() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);

        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", dataDefinition, true);

        // then
        assertEquals("name", viewDefinition.getName());
        assertEquals("plugin", viewDefinition.getPluginIdentifier());
        assertEquals(dataDefinition, viewDefinition.getDataDefinition());
        assertTrue(viewDefinition.isMenuAccessible());
    }

    @Test
    public void shouldReturnPattern() throws Exception {
        // given
        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", mock(DataDefinition.class), true);

        ComponentPattern pattern = Mockito.mock(ComponentPattern.class);
        given(pattern.getName()).willReturn("name");

        viewDefinition.addComponentPattern(pattern);

        // when
        ComponentPattern actualPattern = viewDefinition.getComponentByPath("name");

        // then
        Assert.assertEquals(pattern, actualPattern);
    }

    @Test
    public void shouldReturnPatternByPath() throws Exception {
        // given
        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", mock(DataDefinition.class), true);

        ContainerPattern pattern1 = Mockito.mock(ContainerPattern.class);
        ContainerPattern pattern2 = Mockito.mock(ContainerPattern.class);
        ContainerPattern pattern3 = Mockito.mock(ContainerPattern.class);

        given(pattern1.getName()).willReturn("name1");
        given(pattern1.getChild("name2")).willReturn(pattern2);
        given(pattern2.getChild("name3")).willReturn(pattern3);

        viewDefinition.addComponentPattern(pattern1);

        // when
        ComponentPattern actualPattern = viewDefinition.getComponentByPath("name1.name2.name3");

        // then
        Assert.assertEquals(pattern3, actualPattern);
    }

    @Test
    public void shouldReturnNullWhenPatternNotExists() throws Exception {
        // given
        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", mock(DataDefinition.class), true);

        ContainerPattern pattern = Mockito.mock(ContainerPattern.class);
        given(pattern.getName()).willReturn("name");

        viewDefinition.addComponentPattern(pattern);

        // when
        ComponentPattern actualPattern = viewDefinition.getComponentByPath("name.name2.name3");

        // then
        assertNull(actualPattern);
    }

    @Test
    public void shouldCallInitializeOnChildren() throws Exception {
        // given
        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", mock(DataDefinition.class), true);

        ComponentPattern pattern1 = Mockito.mock(ComponentPattern.class);
        given(pattern1.getName()).willReturn("test1");
        given(pattern1.initialize()).willReturn(false, true);

        ComponentPattern pattern2 = Mockito.mock(ComponentPattern.class);
        given(pattern2.getName()).willReturn("test2");
        given(pattern2.initialize()).willReturn(true);

        viewDefinition.addComponentPattern(pattern1);
        viewDefinition.addComponentPattern(pattern2);

        // when
        viewDefinition.initialize();

        // then
        Mockito.verify(pattern1, times(2)).initialize();
        Mockito.verify(pattern2, times(2)).initialize();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowCyclicDependencyOnInitialize() throws Exception {
        // given
        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", mock(DataDefinition.class), true);

        ComponentPattern pattern1 = Mockito.mock(ComponentPattern.class);
        given(pattern1.getName()).willReturn("test1");
        given(pattern1.initialize()).willReturn(false, false, false);

        ComponentPattern pattern2 = Mockito.mock(ComponentPattern.class);
        given(pattern2.getName()).willReturn("test2");
        given(pattern2.initialize()).willReturn(false, false, false);

        ComponentPattern pattern3 = Mockito.mock(ComponentPattern.class);
        given(pattern3.getName()).willReturn("test3");
        given(pattern3.initialize()).willReturn(false, true);

        ComponentPattern pattern4 = Mockito.mock(ComponentPattern.class);
        given(pattern3.getName()).willReturn("test4");
        given(pattern3.initialize()).willReturn(true);

        viewDefinition.addComponentPattern(pattern1);
        viewDefinition.addComponentPattern(pattern2);
        viewDefinition.addComponentPattern(pattern3);
        viewDefinition.addComponentPattern(pattern4);

        // when
        viewDefinition.initialize();
    }

    @Test
    public void shouldCallEvent() throws Exception {
        // given
        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", mock(DataDefinition.class), true);

        TestEvent event = mock(TestEvent.class);

        ComponentStateMock state = new ComponentStateMock(new JSONObject(of("asd", "123")));
        state.registerTestEvent("eventName", event);

        ComponentPatternMock pattern = new ComponentPatternMock(getComponentDefinition("componentName", viewDefinition), state);

        viewDefinition.addComponentPattern(pattern);

        JSONObject eventJson = new JSONObject();
        eventJson.put(ViewDefinition.JSON_EVENT_NAME, "eventName");
        eventJson.put(ViewDefinition.JSON_EVENT_COMPONENT, "componentName");
        eventJson.put(ViewDefinition.JSON_EVENT_ARGS, new JSONArray(newArrayList("arg1", "arg2")));

        JSONObject contentJson = new JSONObject(of("asd", "qwe"));
        JSONObject componentJson = new JSONObject(of(ComponentState.JSON_CONTENT, contentJson));

        JSONObject json = new JSONObject();
        json.put(ViewDefinition.JSON_EVENT, eventJson);
        json.put(ViewDefinition.JSON_COMPONENTS, new JSONObject(of("componentName", componentJson)));

        // when
        JSONObject result = viewDefinition.performEvent(json, Locale.ENGLISH);

        // then
        assertEquals(contentJson, state.getContent());

        verify(event).invoke(new String[] { "arg1", "arg2" });

        Assert.assertEquals("123", result.getJSONObject("components").getJSONObject("componentName").getJSONObject("content")
                .get("asd"));
    }

    @Test
    public void shouldReturnJsFilePaths() throws Exception {
        // given
        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", mock(DataDefinition.class), true);

        AbstractContainerPattern parent = new WindowComponentPattern(getComponentDefinition("test", viewDefinition));
        ComponentPattern form = new FormComponentPattern(getComponentDefinition("test", parent, viewDefinition));

        parent.addChild(form);

        viewDefinition.addComponentPattern(parent);

        viewDefinition.initialize();

        // when
        Set<String> paths = viewDefinition.getJsFilePaths();

        // then
        Assert.assertEquals(2, paths.size());
    }

    @Test
    public void shouldCallHooks() throws Exception {
        // given
        ViewDefinitionImpl viewDefinition = new ViewDefinitionImpl("name", "plugin", mock(DataDefinition.class), true);

        HookDefinition preInitializeHook = mock(HookDefinition.class);
        viewDefinition.addPreInitializeHook(preInitializeHook);

        HookDefinition postInitializeHook1 = mock(HookDefinition.class);
        viewDefinition.addPostInitializeHook(postInitializeHook1);

        HookDefinition postInitializeHook2 = mock(HookDefinition.class);
        viewDefinition.addPostInitializeHook(postInitializeHook2);

        HookDefinition preRenderHook = mock(HookDefinition.class);
        viewDefinition.addPreRenderHook(preRenderHook);

        JSONObject eventJson = new JSONObject();
        eventJson.put(ViewDefinition.JSON_EVENT_NAME, "eventName");
        eventJson.put(ViewDefinition.JSON_EVENT_ARGS, new JSONArray(newArrayList("arg1", "arg2")));

        JSONObject json = new JSONObject();
        json.put(ViewDefinition.JSON_EVENT, eventJson);
        json.put(ViewDefinition.JSON_COMPONENTS, new JSONObject());

        // when
        viewDefinition.performEvent(json, Locale.ENGLISH);

        // then
        verify(preInitializeHook).callWithViewState(any(ViewDefinitionState.class), eq(Locale.ENGLISH));
        verify(postInitializeHook1).callWithViewState(any(ViewDefinitionState.class), eq(Locale.ENGLISH));
        verify(postInitializeHook2).callWithViewState(any(ViewDefinitionState.class), eq(Locale.ENGLISH));
        verify(preRenderHook).callWithViewState(any(ViewDefinitionState.class), eq(Locale.ENGLISH));
    }

}

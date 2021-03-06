/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.annotation.*;
import com.espertech.esper.client.soda.EPStatementFormatter;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class TestStatementAnnotation extends TestCase
{
    private String NEWLINE = System.getProperty("line.separator");
    private EPServiceProvider epService;

    @MyAnnotationValueEnum(supportEnum = SupportEnum.ENUM_VALUE_1)
    public void testAnnotationSpecificImport() {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addImport(MyAnnotationValueEnum.class);
        configuration.addAnnotationImport(SupportEnum.class);
        configuration.addEventType(SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        runAssertionAnnoImportValidate(epService);

        runAssertionNoClassNameRequired(epService);
    }

    private void runAssertionNoClassNameRequired(EPServiceProvider epService) {
        tryAssertionNoClassNameRequired(epService, SupportEnum.ENUM_VALUE_2, "ENUM_VALUE_2");
        tryAssertionNoClassNameRequired(epService, SupportEnum.ENUM_VALUE_3, "ENUM_value_3");
        tryAssertionNoClassNameRequired(epService, SupportEnum.ENUM_VALUE_1, "enum_value_1");
    }

    private void tryAssertionNoClassNameRequired(EPServiceProvider epService, SupportEnum expected, String text) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("@MyAnnotationValueEnum(supportEnum = " + text + ") select * from SupportBean");
        MyAnnotationValueEnum anno = (MyAnnotationValueEnum) stmt.getAnnotations()[0];
        assertEquals(expected, anno.supportEnum());
    }

    private void runAssertionAnnoImportValidate(EPServiceProvider epService) {
        // init-time import
        epService.getEPAdministrator().createEPL("@MyAnnotationValueEnum(supportEnum = SupportEnum.ENUM_VALUE_1) " +
                "select * from SupportBean");

        // try invalid annotation not yet imported
        String epl = "@MyAnnotationValueEnumTwo(supportEnum = SupportEnum.ENUM_VALUE_1) select * from SupportBean";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Failed to process statement annotations: Failed to resolve @-annotation");

        // runtime import
        epService.getEPAdministrator().getConfiguration().addAnnotationImport(MyAnnotationValueEnumTwo.class.getName());
        epService.getEPAdministrator().createEPL(epl);

        // try invalid use : these are annotation-specific imports of an annotation and an enum
        SupportMessageAssertUtil.tryInvalid(epService, "select * from MyAnnotationValueEnumTwo",
                "Failed to resolve event type: Event type or class named");
        SupportMessageAssertUtil.tryInvalid(epService, "select SupportEnum.ENUM_VALUE_1 from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'SupportEnum.ENUM_VALUE_1'");
    }

    public void testInvalid() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("Bean", SupportBean.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        epService.getEPAdministrator().getConfiguration().addImport("com.espertech.esper.regression.client.*");

        tryInvalid("@MyAnnotationNested(nestableSimple=@MyAnnotationNestableSimple, nestableValues=@MyAnnotationNestableValues, nestableNestable=@MyAnnotationNestableNestable) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationNestableNestable' requires a value for attribute 'value' [@MyAnnotationNested(nestableSimple=@MyAnnotationNestableSimple, nestableValues=@MyAnnotationNestableValues, nestableNestable=@MyAnnotationNestableNestable) select * from Bean]");

        tryInvalid("@MyAnnotationNested(nestableNestable=@MyAnnotationNestableNestable('A'), nestableSimple=1) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationNested' requires a MyAnnotationNestableSimple-typed value for attribute 'nestableSimple' but received a Integer-typed value [@MyAnnotationNested(nestableNestable=@MyAnnotationNestableNestable('A'), nestableSimple=1) select * from Bean]");

        tryInvalid("@MyAnnotationValuePair(stringVal='abc') select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationValuePair' requires a value for attribute 'booleanVal' [@MyAnnotationValuePair(stringVal='abc') select * from Bean]");

        tryInvalid("MyAnnotationValueArray(value=5) select * from Bean", true,
                   "Incorrect syntax near 'MyAnnotationValueArray' [MyAnnotationValueArray(value=5) select * from Bean]");

        tryInvalid("@MyAnnotationValueArray(value=null) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationValueArray' requires a value for attribute 'doubleArray' [@MyAnnotationValueArray(value=null) select * from Bean]");

        tryInvalid("@MyAnnotationValueArray(intArray={},doubleArray={},stringArray={null},value={}) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationValueArray' requires a non-null value for array elements for attribute 'stringArray' [@MyAnnotationValueArray(intArray={},doubleArray={},stringArray={null},value={}) select * from Bean]");

        tryInvalid("@MyAnnotationValueArray(intArray={},doubleArray={},stringArray={1},value={}) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationValueArray' requires a String-typed value for array elements for attribute 'stringArray' but received a Integer-typed value [@MyAnnotationValueArray(intArray={},doubleArray={},stringArray={1},value={}) select * from Bean]");

        tryInvalid("@MyAnnotationValue(value='a', value='a') select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationValue' has duplicate attribute values for attribute 'value' [@MyAnnotationValue(value='a', value='a') select * from Bean]");
        tryInvalid("@ABC select * from Bean", false,
                   "Failed to process statement annotations: Failed to resolve @-annotation class: Could not load annotation class by name 'ABC', please check imports [@ABC select * from Bean]");

        tryInvalid("@MyAnnotationSimple(5) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationSimple' does not have an attribute 'value' [@MyAnnotationSimple(5) select * from Bean]");
        tryInvalid("@MyAnnotationSimple(null) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationSimple' does not have an attribute 'value' [@MyAnnotationSimple(null) select * from Bean]");
        
        tryInvalid("@MyAnnotationValue select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationValue' requires a value for attribute 'value' [@MyAnnotationValue select * from Bean]");

        tryInvalid("@MyAnnotationValue(5) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationValue' requires a String-typed value for attribute 'value' but received a Integer-typed value [@MyAnnotationValue(5) select * from Bean]");
        tryInvalid("@MyAnnotationValueArray(value=\"ABC\", intArray={}, doubleArray={}, stringArray={}) select * from Bean", false,
                   "Failed to process statement annotations: Annotation 'MyAnnotationValueArray' requires a long[]-typed value for attribute 'value' but received a String-typed value [@MyAnnotationValueArray(value=\"ABC\", intArray={}, doubleArray={}, stringArray={}) select * from Bean]");
        tryInvalid("@MyAnnotationValueEnum(a.b.CC) select * from Bean", false,
                   "Annotation enumeration value 'a.b.CC' not recognized as an enumeration class, please check imports or type used [@MyAnnotationValueEnum(a.b.CC) select * from Bean]");

        tryInvalid("@Hint('XXX') select * from Bean", false,
                   "Failed to process statement annotations: Hint annotation value 'XXX' is not one of the known values [@Hint('XXX') select * from Bean]");
        tryInvalid("@Hint('ITERATE_ONLY,XYZ') select * from Bean", false,
                   "Failed to process statement annotations: Hint annotation value 'XYZ' is not one of the known values [@Hint('ITERATE_ONLY,XYZ') select * from Bean]");
        tryInvalid("@Hint('testit=5') select * from Bean", false,
                   "Failed to process statement annotations: Hint annotation value 'testit' is not one of the known values [@Hint('testit=5') select * from Bean]");
        tryInvalid("@Hint('RECLAIM_GROUP_AGED') select * from Bean", false,
                   "Failed to process statement annotations: Hint 'RECLAIM_GROUP_AGED' requires a parameter value [@Hint('RECLAIM_GROUP_AGED') select * from Bean]");
        tryInvalid("@Hint('ITERATE_ONLY,RECLAIM_GROUP_AGED') select * from Bean", false,
                   "Failed to process statement annotations: Hint 'RECLAIM_GROUP_AGED' requires a parameter value [@Hint('ITERATE_ONLY,RECLAIM_GROUP_AGED') select * from Bean]");
        tryInvalid("@Hint('ITERATE_ONLY=5,RECLAIM_GROUP_AGED=5') select * from Bean", false,
                   "Failed to process statement annotations: Hint 'ITERATE_ONLY' does not accept a parameter value [@Hint('ITERATE_ONLY=5,RECLAIM_GROUP_AGED=5') select * from Bean]");
        tryInvalid("@Hint('index(name)xxx') select * from Bean", false,
                    "Failed to process statement annotations: Hint 'INDEX' has additional text after parentheses [@Hint('index(name)xxx') select * from Bean]");
        tryInvalid("@Hint('index') select * from Bean", false,
                    "Failed to process statement annotations: Hint 'INDEX' requires additional parameters in parentheses [@Hint('index') select * from Bean]");
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void tryInvalid(String stmtText, boolean isSyntax, String message)
    {
        try
        {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        }
        catch (EPStatementSyntaxException ex)
        {
            assertTrue(isSyntax);
            assertEquals(message, ex.getMessage());
        }
        catch (EPStatementException ex)
        {
            assertFalse(isSyntax);
            assertEquals(message, ex.getMessage());
        }
    }

    public void testBuiltin()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("Bean", SupportBean.class.getName());
        configuration.addImport(MyAnnotationNestableValues.class.getPackage().getName() + ".*");
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "@Name('MyTestStmt') @Description('MyTestStmt description') @Tag(name=\"UserId\", value=\"value\") select * from Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertTrue((((EPStatementSPI)stmt).isNameProvided()));
        runAssertion(stmt);
        stmt.destroy();
        Name name = (Name) AnnotationUtil.findAnnotation(stmt.getAnnotations(), Name.class);
        assertEquals("MyTestStmt", name.value());

        // try lowercase
        String stmtTextLower = "@name('MyTestStmt') @description('MyTestStmt description') @tag(name=\"UserId\", value=\"value\") select * from Bean";
        stmt = epService.getEPAdministrator().createEPL(stmtTextLower);
        runAssertion(stmt);
        stmt.destroy();

        // try pattern
        stmtText = "@Name('MyTestStmt') @Description('MyTestStmt description') @Tag(name='UserId', value='value') every Bean";
        stmt = epService.getEPAdministrator().createPattern(stmtText);
        runAssertion(stmt);
        stmt.destroy();

        stmtText = "@" + Name.class.getName() + "('MyTestStmt') @Description('MyTestStmt description') @Tag(name=\"UserId\", value=\"value\") every Bean";
        stmt = epService.getEPAdministrator().createPattern(stmtText);
        runAssertion(stmt);

        epService.getEPAdministrator().createEPL("@Hint('ITERATE_ONLY') select * from Bean");
        epService.getEPAdministrator().createEPL("@Hint('ITERATE_ONLY,DISABLE_RECLAIM_GROUP') select * from Bean");
        epService.getEPAdministrator().createEPL("@Hint('ITERATE_ONLY,DISABLE_RECLAIM_GROUP,ITERATE_ONLY') select * from Bean");
        epService.getEPAdministrator().createEPL("@Hint('  iterate_only ') select * from Bean");
        
        // test statement name override
        stmtText = "@Name('MyAnnotatedName') select * from Bean";
        stmt = epService.getEPAdministrator().createEPL(stmtText, "MyABCStmt");
        assertEquals("MyABCStmt", stmt.getName());

        // hint tests
        assertNull(HintEnum.DISABLE_RECLAIM_GROUP.getHint(null));
        assertNull(HintEnum.DISABLE_RECLAIM_GROUP.getHint(new Annotation[0]));

        Annotation[] annos = epService.getEPAdministrator().createEPL("@Hint('DISABLE_RECLAIM_GROUP') select * from Bean").getAnnotations();
        assertEquals("DISABLE_RECLAIM_GROUP", HintEnum.DISABLE_RECLAIM_GROUP.getHint(annos).value());

        annos = epService.getEPAdministrator().createEPL("@Hint('ITERATE_ONLY,ITERATE_ONLY,DISABLE_RECLAIM_GROUP,ITERATE_ONLY') select * from Bean").getAnnotations();
        assertEquals("ITERATE_ONLY,ITERATE_ONLY,DISABLE_RECLAIM_GROUP,ITERATE_ONLY", HintEnum.DISABLE_RECLAIM_GROUP.getHint(annos).value());

        annos = epService.getEPAdministrator().createEPL("@Hint('ITERATE_ONLY,reclaim_group_aged=10') select * from Bean").getAnnotations();
        Hint hint = HintEnum.RECLAIM_GROUP_AGED.getHint(annos);
        assertEquals("10", HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(hint));

        annos = epService.getEPAdministrator().createEPL("@Hint('reclaim_group_aged=11') select * from Bean").getAnnotations();
        hint = HintEnum.RECLAIM_GROUP_AGED.getHint(annos);
        assertEquals("11", HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(hint));

        annos = epService.getEPAdministrator().createEPL("@Hint('index(one, two)') select * from Bean").getAnnotations();
        assertEquals("one, two", HintEnum.INDEX.getHintAssignedValues(annos).get(0));

        // NoLock
        stmt = epService.getEPAdministrator().createEPL("@NoLock select * from Bean");
        assertNotNull(AnnotationUtil.findAnnotation(stmt.getAnnotations(), NoLock.class));
        assertEquals(1, AnnotationUtil.findAnnotations(stmt.getAnnotations(), NoLock.class).size());
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runAssertion(EPStatement stmt)
    {
        Annotation[] annotations = stmt.getAnnotations();
        annotations = sortAlpha(annotations);
        assertEquals(3, annotations.length);

        assertEquals(Description.class, annotations[0].annotationType());
        assertEquals("MyTestStmt description", ((Description)annotations[0]).value());
        assertEquals("@Description(\"MyTestStmt description\")", annotations[0].toString());

        assertEquals(Name.class, annotations[1].annotationType());
        assertEquals("MyTestStmt", ((Name)annotations[1]).value());
        assertEquals("MyTestStmt", stmt.getName());
        assertEquals("@Name(\"MyTestStmt\")", annotations[1].toString());

        assertEquals(Tag.class, annotations[2].annotationType());
        assertEquals("UserId", ((Tag)annotations[2]).name());
        assertEquals("value", ((Tag)annotations[2]).value());
        assertEquals("@Tag(name=\"UserId\", value=\"value\")", annotations[2].toString());

        assertFalse(annotations[2].equals(annotations[1]));
        assertTrue(annotations[1].equals(annotations[1]));
        assertTrue(annotations[1].hashCode() != 0);
    }

    @MyAnnotationSimple
    @MyAnnotationValue("abc")
    @MyAnnotationValuePair(stringVal="a", intVal=-1, longVal=2, booleanVal=true, charVal='x', byteVal=10, shortVal=20, doubleVal=2.5)
    @MyAnnotationValueDefaulted
    @MyAnnotationValueArray(value={1, 2, 3}, intArray={4, 5}, doubleArray={}, stringArray={"X"})
    @MyAnnotationValueEnum(supportEnum = SupportEnum.ENUM_VALUE_3)
    public void testClientAppAnnotationSimple()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("Bean", SupportBean.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        epService.getEPAdministrator().getConfiguration().addImport("com.espertech.esper.regression.client.*");
        epService.getEPAdministrator().getConfiguration().addImport(SupportEnum.class);

        String stmtText =
                "@MyAnnotationSimple " +
                "@MyAnnotationValue('abc') " +
                "@MyAnnotationValueDefaulted " +
                "@MyAnnotationValueEnum(supportEnum=" + SupportEnum.class.getName() + ".ENUM_VALUE_3) " +
                "@MyAnnotationValuePair(stringVal='a',intVal=-1,longVal=2,booleanVal=true,charVal='x',byteVal=10,shortVal=20,doubleVal=2.5) " +
                "@Name('STMTONE') " +
                "select * from Bean";
        String stmtTextFormatted = "@MyAnnotationSimple" + NEWLINE +
                "@MyAnnotationValue('abc')" + NEWLINE +
                "@MyAnnotationValueDefaulted" + NEWLINE +
                "@MyAnnotationValueEnum(supportEnum=" + SupportEnum.class.getName() + ".ENUM_VALUE_3)" + NEWLINE +
                "@MyAnnotationValuePair(stringVal='a',intVal=-1,longVal=2,booleanVal=true,charVal='x',byteVal=10,shortVal=20,doubleVal=2.5)" + NEWLINE +
                "@Name('STMTONE')" + NEWLINE +
                "select *" + NEWLINE +
                "from Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPStatementSPI spi = (EPStatementSPI) stmt;
        assertEquals("select * from Bean", spi.getExpressionNoAnnotations());
        assertTrue(spi.isNameProvided());

        Annotation[] annotations = stmt.getAnnotations();
        annotations = sortAlpha(annotations);
        assertEquals(6, annotations.length);

        assertEquals(MyAnnotationSimple.class, annotations[0].annotationType());
        assertEquals("abc", ((MyAnnotationValue)annotations[1]).value());
        assertEquals("XYZ", ((MyAnnotationValueDefaulted)annotations[2]).value());
        assertEquals("STMTONE", ((Name)annotations[5]).value());

        MyAnnotationValueEnum enumval = (MyAnnotationValueEnum) annotations[3];
        assertEquals(SupportEnum.ENUM_VALUE_2, enumval.supportEnumDef());
        assertEquals(SupportEnum.ENUM_VALUE_3, enumval.supportEnum());

        MyAnnotationValuePair pair = (MyAnnotationValuePair) annotations[4];
        assertEquals("a", pair.stringVal());
        assertEquals(-1, pair.intVal());
        assertEquals(2l, pair.longVal());
        assertEquals(true, pair.booleanVal());
        assertEquals('x', pair.charVal());
        assertEquals(10, pair.byteVal());
        assertEquals(20, pair.shortVal());
        assertEquals(2.5, pair.doubleVal());
        assertEquals("def", pair.stringValDef());
        assertEquals(100, pair.intValDef());
        assertEquals(200l, pair.longValDef());
        assertEquals(true, pair.booleanValDef());
        assertEquals('D', pair.charValDef());
        assertEquals(1.1, pair.doubleValDef());

        // statement model
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        String textFormatted = model.toEPL(new EPStatementFormatter(true));
        assertEquals(stmtTextFormatted, textFormatted);
        EPStatement stmtTwo = epService.getEPAdministrator().create(model);
        assertEquals(stmtTwo.getText(), model.toEPL());
        assertEquals(6, stmtTwo.getAnnotations().length);

        // test array
        stmtText =
                "@MyAnnotationValueArray(value={1, 2, 3}, intArray={4, 5}, doubleArray={}, \nstringArray={\"X\"})\n" +
                "/* Test */ select * \nfrom Bean";
        stmt = epService.getEPAdministrator().createEPL(stmtText);

        annotations = stmt.getAnnotations();
        annotations = sortAlpha(annotations);
        assertEquals(1, annotations.length);

        MyAnnotationValueArray array = (MyAnnotationValueArray) annotations[0];
        assertTrue(Arrays.deepEquals(toObjectArray(array.value()), new Object[] {1L, 2L, 3L}));
        assertTrue(Arrays.deepEquals(toObjectArray(array.intArray()), new Object[] {4, 5}));
        assertTrue(Arrays.deepEquals(toObjectArray(array.doubleArray()), new Object[] {}));
        assertTrue(Arrays.deepEquals(toObjectArray(array.stringArray()), new Object[] {"X"}));
        assertTrue(Arrays.deepEquals(toObjectArray(array.stringArrayDef()), new Object[] {"XYZ"}));

        // statement model
        model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals("@MyAnnotationValueArray(value={1,2,3},intArray={4,5},doubleArray={},stringArray={'X'}) select * from Bean", model.toEPL());
        stmtTwo = epService.getEPAdministrator().create(model);
        assertEquals(stmtTwo.getText(), model.toEPL());
        assertEquals(1, stmtTwo.getAnnotations().length);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testSPI()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("Bean", SupportBean.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        epService.getEPAdministrator().getConfiguration().addImport("com.espertech.esper.regression.client.*");

        String[][] testdata = new String[][] {
                {"@MyAnnotationSimple /* test */ select * from Bean",
                 "/* test */ select * from Bean"},
                {"/* test */ select * from Bean",
                 "/* test */ select * from Bean"},
                {"@MyAnnotationValueArray(value={1, 2, 3}, intArray={4, 5}, doubleArray={}, stringArray={\"X\"})    select * from Bean",
                 "select * from Bean"},
                {"@MyAnnotationSimple\nselect * from Bean",
                 "select * from Bean"},
                {"@MyAnnotationSimple\n@MyAnnotationSimple\nselect * from Bean",
                 "select * from Bean"},
                {"@MyAnnotationValueArray(value={1, 2, 3}, intArray={4, 5}, doubleArray={}, \nstringArray={\"X\"})\n" +
                "/* Test */ select * \nfrom Bean",
                "/* Test */ select * \r\nfrom Bean"},
        };

        for (int i = 0; i < testdata.length; i++)
        {
            EPStatement stmt = epService.getEPAdministrator().createEPL(testdata[i][0]);
            EPStatementSPI spi = (EPStatementSPI) stmt;
            assertEquals("Error on " + testdata[i][0], removeNewlines(testdata[i][1]), removeNewlines(spi.getExpressionNoAnnotations()));
            assertFalse((((EPStatementSPI)stmt).isNameProvided()));
        }

        EPStatement stmt = epService.getEPAdministrator().createEPL(testdata[0][0], "nameProvided");
        assertTrue((((EPStatementSPI)stmt).isNameProvided()));
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    @MyAnnotationNested(
            nestableSimple=@MyAnnotationNestableSimple,
            nestableValues=@MyAnnotationNestableValues(val=999, arr={2, 1}),
            nestableNestable=@MyAnnotationNestableNestable("CDF")
    )
    public void testClientAppAnnotationNested()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("Bean", SupportBean.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        epService.getEPAdministrator().getConfiguration().addImport("com.espertech.esper.regression.client.*");

        String stmtText =
                "@MyAnnotationNested(\n" +
                        "            nestableSimple=@MyAnnotationNestableSimple,\n" +
                        "            nestableValues=@MyAnnotationNestableValues(val=999, arr={2, 1}),\n" +
                        "            nestableNestable=@MyAnnotationNestableNestable(\"CDF\")\n" +
                        "    ) " +
                "select * from Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);

        Annotation[] annotations = stmt.getAnnotations();
        annotations = sortAlpha(annotations);
        assertEquals(1, annotations.length);

        MyAnnotationNested nested = (MyAnnotationNested) annotations[0];
        assertNotNull(nested.nestableSimple());
        assertTrue(Arrays.deepEquals(toObjectArray((nested.nestableValues().arr())), new Object[] {2, 1}));
        assertEquals(999, nested.nestableValues().val());
        assertEquals("CDF", nested.nestableNestable().value());
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private Annotation[] sortAlpha(Annotation[] annotations)
    {
        if (annotations == null)
        {
            return null;
        }
        ArrayList<Annotation> sorted = new ArrayList<Annotation>();
        sorted.addAll(Arrays.asList(annotations));
        Collections.sort(sorted, new Comparator<Annotation>()
        {
            public int compare(Annotation o1, Annotation o2)
            {
                return o1.annotationType().getSimpleName().compareTo(o2.annotationType().getSimpleName());
            }
        });
        return sorted.toArray(new Annotation[sorted.size()]);
    }

    private Object[] toObjectArray(Object array)
    {
        if (!array.getClass().isArray())
        {
            throw new RuntimeException("Parameter passed is not an array");
        }
        Object[] result = new Object[Array.getLength(array)];
        for (int i = 0; i < Array.getLength(array); i++)
        {
            result[i] = Array.get(array, i);
        }
        return result;
    }

    private String removeNewlines(String text) {
        return text.replace("\n", "").replace("\r", "");
    }
}

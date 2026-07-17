package com.pulsewatch.backend.assertion.service;

import com.pulsewatch.backend.assertion.entity.MonitorAssertion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class AssertionEvaluatorTest {

    private AssertionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new AssertionEvaluator();
    }

    @Test
    void testNullOrEmptyAssertions() {
        assertTrue(evaluator.evaluate(null, 200, "body"));
        assertTrue(evaluator.evaluate(Collections.emptyList(), 200, "body"));
    }

    @Test
    void testStatusCodeAssertionPass() {
        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setAssertType("STATUS_CODE");
        assertion.setExpected("200");

        assertTrue(evaluator.evaluate(Collections.singletonList(assertion), 200, "body"));
    }

    @Test
    void testStatusCodeAssertionFail() {
        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setAssertType("STATUS_CODE");
        assertion.setExpected("200");

        assertFalse(evaluator.evaluate(Collections.singletonList(assertion), 500, "body"));
    }

    @Test
    void testBodyContainsAssertionPass() {
        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setAssertType("BODY_CONTAINS");
        assertion.setExpected("success");

        assertTrue(evaluator.evaluate(Collections.singletonList(assertion), 200, "{\"status\":\"success\"}"));
    }

    @Test
    void testBodyContainsAssertionFail() {
        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setAssertType("BODY_CONTAINS");
        assertion.setExpected("success");

        assertFalse(evaluator.evaluate(Collections.singletonList(assertion), 200, "{\"status\":\"error\"}"));
    }

    @Test
    void testBodyContainsAssertionFailNullBody() {
        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setAssertType("BODY_CONTAINS");
        assertion.setExpected("success");

        assertFalse(evaluator.evaluate(Collections.singletonList(assertion), 200, null));
    }

    @Test
    void testBodyNotContainsAssertionPass() {
        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setAssertType("BODY_NOT_CONTAINS");
        assertion.setExpected("error");

        assertTrue(evaluator.evaluate(Collections.singletonList(assertion), 200, "{\"status\":\"success\"}"));
        // Null body doesn't contain anything, so it passes
        assertTrue(evaluator.evaluate(Collections.singletonList(assertion), 200, null));
    }

    @Test
    void testBodyNotContainsAssertionFail() {
        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setAssertType("BODY_NOT_CONTAINS");
        assertion.setExpected("error");

        assertFalse(evaluator.evaluate(Collections.singletonList(assertion), 200, "{\"status\":\"error\"}"));
    }

    @Test
    void testMultipleAssertions() {
        MonitorAssertion a1 = new MonitorAssertion();
        a1.setAssertType("STATUS_CODE");
        a1.setExpected("200");

        MonitorAssertion a2 = new MonitorAssertion();
        a2.setAssertType("BODY_CONTAINS");
        a2.setExpected("OK");

        // Both pass
        assertTrue(evaluator.evaluate(Arrays.asList(a1, a2), 200, "Response is OK"));
        
        // a1 fails
        assertFalse(evaluator.evaluate(Arrays.asList(a1, a2), 404, "Response is OK"));
        
        // a2 fails
        assertFalse(evaluator.evaluate(Arrays.asList(a1, a2), 200, "Response is FAIL"));
    }

    @Test
    void testUnknownAssertionType() {
        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setAssertType("UNKNOWN_TYPE");
        assertion.setExpected("foo");

        // Should log warning and treat as pass
        assertTrue(evaluator.evaluate(Collections.singletonList(assertion), 200, "body"));
    }
}

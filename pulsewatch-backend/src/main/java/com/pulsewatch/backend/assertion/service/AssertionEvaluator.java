package com.pulsewatch.backend.assertion.service;

import com.pulsewatch.backend.assertion.entity.MonitorAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Evaluates a list of assertions against a completed HTTP check.
 * All assertions must pass for the check to be considered successful.
 *
 * Supported types:
 *   STATUS_CODE  + EQ         — HTTP status code equals expected (e.g. "200")
 *   BODY_CONTAINS + CONTAINS  — response body contains the expected string
 *   BODY_NOT_CONTAINS + NOT_CONTAINS — response body does NOT contain the expected string
 */
@Service
public class AssertionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(AssertionEvaluator.class);
    private static final int MAX_BODY_LOG_LENGTH = 200;

    /**
     * @param assertions  assertions configured for this monitor (may be empty)
     * @param statusCode  HTTP status code received
     * @param body        response body as string (may be null if capture failed)
     * @return true if all assertions pass (or list is empty); false if any fail
     */
    public boolean evaluate(List<MonitorAssertion> assertions, int statusCode, String body) {
        if (assertions == null || assertions.isEmpty()) {
            return true; // no assertions = pass
        }

        for (MonitorAssertion assertion : assertions) {
            if (!evaluateSingle(assertion, statusCode, body)) {
                logger.debug("Assertion FAILED — type={} operator={} expected='{}' statusCode={} body='{}'",
                        assertion.getAssertType(), assertion.getOperator(), assertion.getExpected(),
                        statusCode, truncate(body));
                return false;
            }
        }
        return true;
    }

    private boolean evaluateSingle(MonitorAssertion assertion, int statusCode, String body) {
        String type = assertion.getAssertType();
        String expected = assertion.getExpected();

        return switch (type) {
            case "STATUS_CODE" -> String.valueOf(statusCode).equals(expected.trim());

            case "BODY_CONTAINS" -> {
                if (body == null) yield false;
                yield body.contains(expected);
            }

            case "BODY_NOT_CONTAINS" -> {
                if (body == null) yield true; // empty body doesn't contain anything
                yield !body.contains(expected);
            }

            default -> {
                logger.warn("Unknown assertion type '{}' — treating as pass", type);
                yield true;
            }
        };
    }

    private String truncate(String s) {
        if (s == null) return "null";
        return s.length() > MAX_BODY_LOG_LENGTH ? s.substring(0, MAX_BODY_LOG_LENGTH) + "..." : s;
    }
}

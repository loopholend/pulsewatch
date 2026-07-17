package com.pulsewatch.backend.assertion.dto;

public class MonitorAssertionRequest {
    @jakarta.validation.constraints.NotBlank(message = "Assertion type is required")
    @jakarta.validation.constraints.Pattern(regexp = "STATUS_CODE|BODY_CONTAINS|BODY_NOT_CONTAINS", message = "Invalid assertion type")
    private String assertType;

    @jakarta.validation.constraints.NotBlank(message = "Operator is required")
    @jakarta.validation.constraints.Pattern(regexp = "EQ|CONTAINS|NOT_CONTAINS", message = "Invalid operator")
    private String operator;

    @jakarta.validation.constraints.NotBlank(message = "Expected value is required")
    private String expected;

    public String getAssertType() { return assertType; }
    public void setAssertType(String assertType) { this.assertType = assertType; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
}

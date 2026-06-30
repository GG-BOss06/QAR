package com.qar.securitysystem;

import com.qar.securitysystem.abe.AttributePolicyEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class AttributePolicyEvaluatorTests {

    private final AttributePolicyEvaluator evaluator = new AttributePolicyEvaluator();

    @Test
    void orPolicyConsumesRemainingTokensWhenLeftSideMatches() {
        boolean allowed = evaluator.evaluate(
                "personNo:20260001 OR role:admin",
                Set.of("personno:20260001", "role:user")
        );

        Assertions.assertTrue(allowed);
    }

    @Test
    void andPolicyConsumesRemainingTokensWhenLeftSideFails() {
        boolean allowed = evaluator.evaluate(
                "department:飞行一部 AND role:admin",
                Set.of("department:飞行一部", "role:user")
        );

        Assertions.assertFalse(allowed);
    }
}

package com.skillforge.skillforge.dto;

import java.math.BigDecimal;
import java.util.List;

public class StudentMatchDto {

    private final StudentSummaryDto student;
    private final BigDecimal matchPercentage;
    private final List<String> matchedSkills;
    private final boolean matchingAvailable;

    public StudentMatchDto(
            StudentSummaryDto student,
            BigDecimal matchPercentage,
            List<String> matchedSkills,
            boolean matchingAvailable) {

        this.student = student;
        this.matchPercentage = matchPercentage;
        this.matchedSkills = matchedSkills == null
                ? List.of()
                : List.copyOf(matchedSkills);
        this.matchingAvailable = matchingAvailable;
    }

    public StudentSummaryDto getStudent() {
        return student;
    }

    public BigDecimal getMatchPercentage() {
        return matchPercentage;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public boolean isMatchingAvailable() {
        return matchingAvailable;
    }
}
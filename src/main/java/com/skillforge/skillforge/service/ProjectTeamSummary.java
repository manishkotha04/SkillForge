
package com.skillforge.skillforge.service;

import java.util.List;

public record ProjectTeamSummary(
        Long projectId,
        long teammateCount,
        List<String> teammateNames
) {
}

package com.ac.mcp.server.service;

import com.ac.mcp.server.domain.Activity;
import com.ac.mcp.server.domain.ActivityStat;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class ActivityService {
    public List<Activity> list(String personId, Instant start, Instant end) {
        return List.of(new Activity("a-1", personId, "北京", start), new Activity("a-2", personId, "上海", end));
    }
    public List<ActivityStat> statistics(String personId, Instant start, Instant end) {
        return List.of(new ActivityStat("北京", 18), new ActivityStat("上海", 11), new ActivityStat("杭州", 6));
    }
}

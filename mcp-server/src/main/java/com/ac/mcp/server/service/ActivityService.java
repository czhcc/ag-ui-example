package com.ac.mcp.server.service;

import com.ac.mcp.server.domain.Activity;
import com.ac.mcp.server.domain.ActivityStat;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic mock dataset for drill-down testing: multi-city, multi-date,
 * multi-event-type activities generated from fixed seeds (no randomness, so
 * statistics and drill-down lists always reconcile).
 */
@Service
public class ActivityService {

    private static final Map<String, List<String>> CITY_EVENTS = Map.of(
            "北京", List.of("客户会议@国贸三期|BUSINESS|李四", "产品评审@中关村|WORK|王五",
                    "签约仪式@金融街|BUSINESS|赵六", "技术分享@西二旗|WORK|null",
                    "商务晚宴@三里屯|BUSINESS|李四", "展会参观@亦庄|BUSINESS|null",
                    "部门团建@怀柔|SOCIAL|王五", "高铁差旅@北京南站|TRAVEL|null"),
            "上海", List.of("渠道洽谈@陆家嘴|BUSINESS|钱七", "用户调研@静安寺|WORK|null",
                    "合作伙伴拜访@张江|BUSINESS|孙八", "行业峰会@虹桥|SOCIAL|null",
                    "晚餐会议@外滩|BUSINESS|钱七"),
            "杭州", List.of("阿里交流@西溪园区|BUSINESS|周九", "供应商审核@滨江|WORK|null",
                    "西湖商务餐@西湖区|BUSINESS|周九")
    );

    private final List<Activity> store = buildStore();

    private static List<Activity> buildStore() {
        List<Activity> activities = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int seq = 0;
        for (var cityEntry : CITY_EVENTS.entrySet()) {
            String city = cityEntry.getKey();
            List<String> events = cityEntry.getValue();
            for (int i = 0; i < events.size(); i++) {
                String[] parts = events.get(i).split("\\|");
                String[] placeDesc = parts[0].split("@");
                LocalDate day = today.minusDays(2L * i + city.hashCode() % 3);
                activities.add(new Activity(
                        "a-" + (++seq), "person-001", city,
                        day.atTime(9 + (i % 9), (i * 17) % 60).toInstant(ZoneOffset.ofHours(8)),
                        parts[1], placeDesc[0] + "（" + city + "·" + placeDesc[1] + "）",
                        "null".equals(parts[2]) ? null : parts[2]));
            }
        }
        return List.copyOf(activities);
    }

    public List<Activity> list(String personId, Instant start, Instant end) {
        return list(personId, start, end, null);
    }

    public List<Activity> list(String personId, Instant start, Instant end, String city) {
        return store.stream()
                .filter(it -> it.personId().equals(personId))
                .filter(it -> start == null || !it.occurredAt().isBefore(start))
                .filter(it -> end == null || it.occurredAt().isAfter(end))
                .filter(it -> city == null || city.isBlank() || it.city().equals(city))
                .toList();
    }

    public List<ActivityStat> statistics(String personId, Instant start, Instant end) {
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (Activity activity : list(personId, start, end)) {
            counts.merge(activity.city(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(e -> new ActivityStat(e.getKey(), e.getValue()))
                .toList();
    }
}

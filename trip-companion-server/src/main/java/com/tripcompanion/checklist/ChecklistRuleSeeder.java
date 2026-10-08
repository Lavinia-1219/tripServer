package com.tripcompanion.checklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ChecklistRuleSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChecklistRuleSeeder.class);

    private final ChecklistRuleRepository ruleRepository;

    public ChecklistRuleSeeder(ChecklistRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existing = ruleRepository.count();
        if (existing > 0) {
            log.info("规则表已有 {} 条规则，跳过初始化", existing);
            return;
        }

        List<ChecklistRule> defaults = defaultRules();
        ruleRepository.saveAll(defaults);
        log.info("规则表是空的，已写入 {} 条默认规则", defaults.size());
    }

    static List<ChecklistRule> defaultRules() {
        return List.of(
                rule("wet", "EQ", 1, "雨伞", "雨具", "行程期间有雨雪", 10),
                rule("wet", "EQ", 1, "防水鞋套", "雨具", "雨雪天鞋子容易湿", 11),

                rule("tempMin", "LT", 0, "暖宝宝", "保暖", "最低温跌破 0℃", 20),
                rule("tempMin", "LE", 5, "羽绒服", "衣物", "最低温 ≤5℃，很冷", 21),
                rule("tempMin", "LE", 10, "厚外套", "衣物", "最低温 ≤10℃，偏冷", 22),
                rule("tempMin", "LE", 15, "长袖外套", "衣物", "最低温 ≤15℃，早晚凉", 23),

                rule("tempMax", "GE", 30, "短袖T恤", "衣物", "最高温 ≥30℃，很热", 30),
                rule("tempMax", "GE", 32, "遮阳帽", "防晒", "高温暴晒，注意遮阳", 31),

                rule("sunny", "EQ", 1, "防晒霜", "防晒", "晴天且气温高，紫外线强", 40),
                rule("sunny", "EQ", 1, "墨镜", "防晒", "晴天且气温高，注意晃眼", 41),

                rule("humidity", "GE", 85, "速干衣", "衣物", "湿度 ≥85%，衣服不容易干", 50)
        );
    }

    private static ChecklistRule rule(String field, String operator, double threshold,
                                      String itemName, String category, String reason, int priority) {
        ChecklistRule r = new ChecklistRule(field, operator, threshold, itemName);
        r.setCategory(category);
        r.setReason(reason);
        r.setPriority(priority);
        r.setEnabled(true);
        return r;
    }
}

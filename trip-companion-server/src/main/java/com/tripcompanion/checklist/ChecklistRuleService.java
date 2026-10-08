package com.tripcompanion.checklist;

import com.tripcompanion.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ChecklistRuleService {

    private final ChecklistRuleRepository ruleRepository;

    public ChecklistRuleService(ChecklistRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    public List<ChecklistDtos.RuleView> list() {
        return ruleRepository.findAllByOrderByPriorityAscIdAsc().stream()
                .map(ChecklistRuleService::toView)
                .toList();
    }

    @Transactional
    public ChecklistDtos.RuleView create(ChecklistDtos.RuleRequest req) {
        String field = req.field() == null ? "" : req.field().trim();
        String operator = req.operator() == null ? "" : req.operator().trim().toUpperCase(Locale.ROOT);

        if (!RuleEngine.supportedFields().contains(field)) {
            throw ApiException.badRequest("BAD_RULE_FIELD",
                    "不认识的条件字段：" + field + "。可用：" + RuleEngine.supportedFields());
        }
        if (!RuleEngine.supportedOperators().contains(operator)) {
            throw ApiException.badRequest("BAD_RULE_OPERATOR",
                    "不认识的比较符：" + operator + "。可用：" + RuleEngine.supportedOperators());
        }

        ChecklistRule rule = new ChecklistRule(field, operator, req.threshold(), req.itemName().trim());
        rule.setCategory(req.category());
        rule.setReason(req.reason());
        rule.setPriority(req.priority() == null ? 100 : req.priority());
        rule.setEnabled(true);

        return toView(ruleRepository.save(rule));
    }

    @Transactional
    public ChecklistDtos.RuleView setEnabled(Long ruleId, boolean enabled) {
        ChecklistRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> ApiException.notFound("规则不存在：" + ruleId));
        rule.setEnabled(enabled);
        return toView(ruleRepository.save(rule));
    }

    private static ChecklistDtos.RuleView toView(ChecklistRule rule) {
        return new ChecklistDtos.RuleView(
                rule.getId(),
                rule.getField(),
                rule.getOperator(),
                rule.getThreshold(),
                rule.getItemName(),
                rule.getCategory(),
                rule.getReason(),
                rule.getPriority(),
                rule.getEnabled(),
                RuleEngine.describe(rule));
    }
}

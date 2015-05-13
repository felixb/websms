package de.ub0r.android.websms.rules;

public class IndexedRule {
    private final int idx;
    private final Rule rule;

    public IndexedRule(int idx, Rule rule) {
        this.idx = idx;
        this.rule = rule;
    }

    public int getIdx() {
        return this.idx;
    }

    public Rule getRule() {
        return this.rule;
    }

}

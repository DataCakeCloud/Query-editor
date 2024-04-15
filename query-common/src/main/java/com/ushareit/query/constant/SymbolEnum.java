package com.ushareit.query.constant;

/**
 * @author swq
 * @date 2018/11/9
 */
public enum SymbolEnum {
    /**
     * 半角符号
     */
    CCOMMA(",", "半角逗号"),
    PERIOD(".", "半角句号"),
    SEMICOLON(";", "半角分号"),
    COLON(":", "半角冒号"),
    DOUBLE_SEMICOLON(";;", "双半角分号"),
    APOSTROPHE("'", "半角单引号"),
    BREAK_LINE_SYMBOL("\n", "换行符"),
    UNDERLINE("_", "下划线"),
    DOUBLE_UNDERLINE("__", "双下划线"),
    DASH("-", "中划线"),
    STAR("*", "星号"),
    EMPTY("", "空字符串"),
    QUESTION_MARK("?", "问号字符串"),
    SPACE(" ", "空格"),
    SLASH("/","斜线"),
    ;

    private String symbol;
    private String explanation;

    SymbolEnum(String symbol, String explanation) {
        this.symbol = symbol;
        this.explanation = explanation;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public String getExplanation() {
        return this.explanation;
    }
}

package oasys.migration.alias;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;

/**
 * SQLFluff-like (indented) formatter for migration outputs.
 *
 * Focus:
 * - Clause layout: WITH/SELECT/FROM/WHERE/GROUP BY/ORDER BY/JOIN/ON
 * - Indentation: 4 spaces
 * - SELECT list: newline per item
 * - Conditions: AND/OR on new lines (including inside parenthesis)
 * - Subquery in parenthesis: IN ( SELECT ... ) / THEN ( SELECT ... ) => indented block + aligned ')'
 * - CASE blocks formatted (CASE/WHEN/THEN/ELSE/END)
 * - Function-arg parentheses: SQLFluff style multiline args (with maxLineLen heuristic)
 *
 * PATCH:
 * - Fix duplicated "THEN THEN" / "ELSE ELSE"
 * - Align subquery/cond-group parens by current line indent (works inside CASE)
 * - Inline ONLY small numeric literal for CASE: "THEN 4" / "ELSE 3"
 * - Compact last simple literal in multiline function args: "... END, 0 )"
 *
 * CASE style:
 * - CASE: keep "WHEN <cond> THEN" on same line (expression goes to next line)
 * - blank line between branches (default OFF)
 * - Standalone line comments ("-- ...") are re-indented to current context indent (esp. inside CASE)
 *
 * ADDED:
 * - Inline single-condition WHERE/HAVING/ON: "WHERE x = y" (no AND/OR at top-level)
 * - Tight semicolon spacing: "FROM BASE ;" -> "FROM BASE;"
 *
 * REFACTOR (1~4 적용):
 * 1) format() 비대함 분리: State + handler 방식
 * 2) 키워드 equals 체인 -> Set / switch
 * 3) MyBatis 바인딩 #{...}, ${...} 를 단일 토큰으로 consume
 * 4) MERGE 지원: ON (...) 조건절 종료 키워드 WHEN 처리(단, CASE 내부 제외)
 *
 * Not a full SQL parser; best-effort formatter for typical Oracle/MyBatis SQL.
 */
public final class SqlPrettifier {

    private SqlPrettifier() {}

    private static final String IND = "    ";

    // ========================= Options =========================

    // Options (default ON)
    private static final boolean OPT_FUNC_ARGS =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.funcArgs", "true"));
    private static final boolean OPT_CASE_ELSE_NEWLINE =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.caseElseNewline", "true"));
    private static final boolean OPT_CASE_THEN_NEWLINE =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.caseThenNewline", "true"));
    private static final int OPT_MAX_LINE_LEN =
            parseInt(System.getProperty("oasys.migration.pretty.maxLineLen", "120"), 120);

    // PATCH options
    private static final boolean OPT_CASE_INLINE_SIMPLE =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.caseInlineSimple", "true"));
    private static final boolean OPT_FUNC_ARGS_COMPACT_LAST_LITERAL =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.funcArgsCompactLastLiteral", "true"));

    // CASE style options
    private static final boolean OPT_CASE_WHEN_THEN_SAME_LINE =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.caseWhenThenSameLine", "true"));

    // ✅ default OFF (requested: remove blank line between WHEN/ELSE)
    private static final boolean OPT_CASE_BLANK_LINE_BETWEEN_BRANCHES =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.caseBlankLineBetweenBranches", "false"));

    private static final boolean OPT_REINDENT_STANDALONE_LINE_COMMENTS =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.reindentStandaloneLineComments", "true"));

    // ADDED options
    private static final boolean OPT_INLINE_SINGLE_COND_WHERE =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.whereInlineSingle", "true"));
    private static final boolean OPT_TIGHT_SEMICOLON =
            Boolean.parseBoolean(System.getProperty("oasys.migration.pretty.semicolonTight", "true"));

    // ========================= Keyword Sets =========================

    private static final Set<String> JOIN_MODIFIERS = Set.of(
            "INNER", "LEFT", "RIGHT", "FULL", "CROSS"
    );

    private static final Set<String> CLAUSE_KEYWORDS = Set.of(
            "WITH",
            "SELECT", "FROM", "WHERE", "GROUP", "ORDER", "HAVING",
            "UNION", "INTERSECT", "EXCEPT", "MINUS",
            "INSERT", "INTO", "VALUES",
            "UPDATE", "SET",
            "DELETE",
            "MERGE",
            "USING", "ON",
            // CASE tokens included to keep compatibility, but handled separately
            "WHEN", "THEN", "ELSE", "END"
    );

    // 이전 토큰이 이 키워드면, tightParen이라도 함수 호출로 보지 않음
    private static final Set<String> PAREN_KEYWORDS = Set.of(
            "IN", "EXISTS", "NOT", "VALUES", "CAST", "OVER", "THEN", "WHEN", "ELSE", "AS"
    );

    // ========================= Context Classes =========================

    private static final class CaseCtx {
        final int parenDepthAtStart;
        final int baseIndent;
        final int innerIndent;
        boolean whenLineActive;
        boolean seenBranch;

        CaseCtx(int parenDepthAtStart, int baseIndent) {
            this.parenDepthAtStart = parenDepthAtStart;
            this.baseIndent = Math.max(0, baseIndent);
            this.innerIndent = this.baseIndent + 1;
            this.whenLineActive = false;
            this.seenBranch = false;
        }
    }

    private static final class PrettyParenCtx {
        final int parenDepth;        // depth AFTER '('
        final int closeIndentLevel;  // indent for ')'

        PrettyParenCtx(int parenDepth, int closeIndentLevel) {
            this.parenDepth = parenDepth;
            this.closeIndentLevel = Math.max(0, closeIndentLevel);
        }
    }

    private static final class FuncCtx {
        final int parenDepth;     // depth AFTER '('
        final int baseIndent;     // indent where function name starts
        final int argIndent;      // indent for each argument line

        FuncCtx(int parenDepth, int baseIndent) {
            this.parenDepth = parenDepth;
            this.baseIndent = Math.max(0, baseIndent);
            this.argIndent = this.baseIndent + 1;
        }
    }

    private static final class IndentCtx {
        final int parenDepth; // depth AFTER '('
        final int offset;     // can be negative

        IndentCtx(int parenDepth, int offset) {
            this.parenDepth = parenDepth;
            this.offset = offset;
        }
    }

    private static final class State {
        final String s;
        final int n;
        final StringBuilder out;

        int i = 0;
        int depth = 0;
        int activeOffset = 0;

        // stacks
        final Deque<Integer> selectListDepth = new ArrayDeque<>();
        final Deque<Integer> condDepth = new ArrayDeque<>();
        final Deque<Integer> inlineCondDepth = new ArrayDeque<>();
        final Deque<Integer> setDepth  = new ArrayDeque<>();
        final Deque<Integer> valuesDepth = new ArrayDeque<>();
        final Deque<Integer> intoColsDepth = new ArrayDeque<>();

        final Deque<CaseCtx> caseStack = new ArrayDeque<>();
        final Deque<PrettyParenCtx> prettyParens = new ArrayDeque<>();
        final Deque<FuncCtx> funcStack = new ArrayDeque<>();
        final Deque<IndentCtx> indentStack = new ArrayDeque<>();

        boolean afterInto = false;
        boolean afterValues = false;

        // WITH(CTE) context
        boolean inWith = false;
        int withBaseDepth = 0;

        // SELECT first item newline
        boolean pendingSelectFirstItemNewline = false;
        int pendingSelectIndent = 0;

        // MERGE context (best-effort)
        boolean inMerge = false;

        State(String sql) {
            this.s = sql;
            this.n = sql.length();
            this.out = new StringBuilder(sql.length() + 256);
        }
    }

    // ========================= Public API =========================

    public static String format(String sql) {
        if (sql == null || sql.isBlank()) return sql;

        State st = new State(sql);

        while (st.i < st.n) {
            char c = st.s.charAt(st.i);

            // 0) Tight semicolon
            if (c == ';') { handleSemicolon(st); continue; }

            // 1) Comments / strings / MyBatis bindings
            if (isLineCommentStart(st, c)) { handleLineComment(st); continue; }
            if (isBlockCommentStart(st, c)) { handleBlockComment(st); continue; }
            if (c == '\'') { handleStringLiteral(st); continue; }
            if (isMyBatisBindingStart(st, c)) { handleMyBatisBinding(st); continue; }

            // 2) Pending select first item newline
            if (st.pendingSelectFirstItemNewline && !Character.isWhitespace(c)) {
                st.out.append('\n');
                appendIndent(st.out, st.pendingSelectIndent);
                st.pendingSelectFirstItemNewline = false;
            }

            // 3) Structural chars
            if (c == '(') { handleOpenParen(st); continue; }
            if (c == ')') { handleCloseParen(st); continue; }
            if (c == ',') { handleComma(st); continue; }
            if (Character.isWhitespace(c)) { handleWhitespace(st, c); continue; }

            // 4) Word token or default char
            if (isWordStart(c)) { handleWordToken(st); continue; }

            // default single char
            st.out.append(c);
            st.i++;
        }

        return st.out.toString();
    }

    // ========================= Handlers =========================

    private static void handleSemicolon(State st) {
        if (OPT_TIGHT_SEMICOLON) rtrimSpaces(st.out);
        st.out.append(';');
        st.i++;
        // remove spaces/tabs/CR right after ';' (keep newline)
        st.i = skipSpacesTabsCrOnly(st.s, st.i, st.n);

        // statement end: best-effort reset merge flag
        st.inMerge = false;
    }

    private static boolean isLineCommentStart(State st, char c) {
        return c == '-' && st.i + 1 < st.n && st.s.charAt(st.i + 1) == '-';
    }

    private static void handleLineComment(State st) {
        int start = st.i;
        st.i += 2;
        while (st.i < st.n && st.s.charAt(st.i) != '\n') st.i++;

        // reindent only if standalone comment line (only whitespace since last '\n')
        if (OPT_REINDENT_STANDALONE_LINE_COMMENTS && isOnlyIndentSinceNewline(st.out)) {
            int pref = preferredIndentLevel(st);
            int lineStart = lastNewlinePos(st.out) + 1;
            st.out.setLength(lineStart);
            appendIndent(st.out, pref);
        }

        st.out.append(st.s, start, st.i);
    }

    private static boolean isBlockCommentStart(State st, char c) {
        return c == '/' && st.i + 1 < st.n && st.s.charAt(st.i + 1) == '*';
    }

    private static void handleBlockComment(State st) {
        int start = st.i;
        st.i += 2;
        while (st.i + 1 < st.n) {
            if (st.s.charAt(st.i) == '*' && st.s.charAt(st.i + 1) == '/') { st.i += 2; break; }
            st.i++;
        }
        st.out.append(st.s, start, st.i);
    }

    private static void handleStringLiteral(State st) {
        int start = st.i;
        st.i++;
        while (st.i < st.n) {
            char x = st.s.charAt(st.i++);
            if (x == '\'') {
                if (st.i < st.n && st.s.charAt(st.i) == '\'') { st.i++; continue; } // escaped ''
                break;
            }
        }
        st.out.append(st.s, start, st.i);
    }

    // 3) MyBatis binding: "#{...}" or "${...}" as one token
    private static boolean isMyBatisBindingStart(State st, char c) {
        if ((c != '#' && c != '$') || st.i + 1 >= st.n) return false;
        return st.s.charAt(st.i + 1) == '{';
    }

    private static void handleMyBatisBinding(State st) {
        int start = st.i;
        st.i += 2; // skip "#{", "${"
        while (st.i < st.n) {
            char c = st.s.charAt(st.i++);
            if (c == '}') break;
        }
        st.out.append(st.s, start, st.i);
    }

    private static void handleWhitespace(State st, char c) {
        // CASE 안에서는 연속 개행(빈 줄) 제거: "\n\n" -> "\n"
        if (!st.caseStack.isEmpty() && c == '\n') {
            int len = st.out.length();
            if (len > 0 && st.out.charAt(len - 1) == '\n') {
                st.i++;
                return;
            }
        }
        st.out.append(c);
        st.i++;
    }

    private static void handleOpenParen(State st) {
        int openPos = st.i;
        int beforeDepth = st.depth;

        boolean tightParen = (openPos - 1 >= 0) && !Character.isWhitespace(st.s.charAt(openPos - 1));

        st.out.append('(');
        st.depth++;
        st.i++;

        // INSERT INTO ... (col,...)
        if (st.afterInto) {
            st.intoColsDepth.push(st.depth);
            st.afterInto = false;

            st.out.append('\n');
            appendIndent(st.out, ind(st.depth, st.activeOffset));
            st.i = skipWsOnly(st.s, st.i, st.n);
            return;
        }

        // VALUES (...)
        if (st.afterValues) {
            st.valuesDepth.push(st.depth);
            st.afterValues = false;

            st.out.append('\n');
            appendIndent(st.out, ind(st.depth, st.activeOffset));
            st.i = skipWsOnly(st.s, st.i, st.n);
            return;
        }

        boolean inCond = !st.condDepth.isEmpty() && beforeDepth >= st.condDepth.peek();
        boolean inInlineCond = isInlineCondActive(st.inlineCondDepth, beforeDepth);

        String prevWord = prevWordUpper(st.s, openPos);
        boolean looksLikeFunctionCall = tightParen && (prevWord != null && !PAREN_KEYWORDS.contains(prevWord));

        boolean subqueryStarts = startsWithKeywordAfterParen(st.s, st.i, st.n, "SELECT");
        boolean caseStarts     = startsWithKeywordAfterParen(st.s, st.i, st.n, "CASE");

        // function args: SQLFluff-like multiline (with maxLineLen heuristic)
        if (OPT_FUNC_ARGS && looksLikeFunctionCall) {
            boolean forceMultiline = subqueryStarts || caseStarts
                    || containsKeywordInParenBody(st.s, st.i, st.n, "SELECT", st.depth)
                    || containsKeywordInParenBody(st.s, st.i, st.n, "CASE", st.depth)
                    || containsKeywordInParenBody(st.s, st.i, st.n, "WITH", st.depth);

            boolean shortEnough = isFunctionCallShortEnough(st.s, openPos, st.i, st.n, st.depth, OPT_MAX_LINE_LEN);
            boolean doMultiline = forceMultiline || !shortEnough;

            if (doMultiline) {
                int baseIndent = currentLineIndentLevel(st.out);

                st.prettyParens.push(new PrettyParenCtx(st.depth, baseIndent));
                st.funcStack.push(new FuncCtx(st.depth, baseIndent));

                int j = skipWsOnly(st.s, st.i, st.n);
                if (j < st.n && st.s.charAt(j) != ')') {
                    st.out.append('\n');
                    appendIndent(st.out, st.funcStack.peek().argIndent);
                    st.i = j;
                } else {
                    st.i = j;
                }
                return;
            }
        }

        // condition grouping / subquery / (CASE ...) => indent based on current line indent
        if (!looksLikeFunctionCall && ((inCond && !inInlineCond) || subqueryStarts || caseStarts)) {
            int baseIndent = currentLineIndentLevel(st.out); // indent where '(' is placed

            // align ')'
            st.prettyParens.push(new PrettyParenCtx(st.depth, baseIndent));

            // inside indent = baseIndent + 1
            int desiredInsideIndent = baseIndent + 1;
            int desiredActiveOffset = desiredInsideIndent - st.depth;
            int delta = desiredActiveOffset - st.activeOffset;

            st.indentStack.push(new IndentCtx(st.depth, delta));
            st.activeOffset += delta;

            st.out.append('\n');
            appendIndent(st.out, desiredInsideIndent);
            st.i = skipWsOnly(st.s, st.i, st.n);
            return;
        }

        // default: keep inline
    }

    private static void handleCloseParen(State st) {
        // pop function ctx
        if (!st.funcStack.isEmpty() && st.funcStack.peek().parenDepth == st.depth) {
            st.funcStack.pop();
        }

        // align close paren if pretty
        if (!st.prettyParens.isEmpty() && st.prettyParens.peek().parenDepth == st.depth) {
            ensureLineStart(st.out, st.prettyParens.peek().closeIndentLevel);
            st.prettyParens.pop();
        }

        st.out.append(')');

        st.depth = Math.max(0, st.depth - 1);

        while (!st.indentStack.isEmpty() && st.indentStack.peek().parenDepth > st.depth) {
            st.activeOffset -= st.indentStack.pop().offset;
        }

        while (!st.selectListDepth.isEmpty() && st.selectListDepth.peek() > st.depth) st.selectListDepth.pop();
        while (!st.condDepth.isEmpty() && st.condDepth.peek() > st.depth) st.condDepth.pop();
        while (!st.inlineCondDepth.isEmpty() && st.inlineCondDepth.peek() > st.depth) st.inlineCondDepth.pop();
        while (!st.setDepth.isEmpty() && st.setDepth.peek() > st.depth) st.setDepth.pop();

        // ✅ important: close INSERT/VALUES column/value list contexts
        while (!st.intoColsDepth.isEmpty() && st.intoColsDepth.peek() > st.depth) st.intoColsDepth.pop();
        while (!st.valuesDepth.isEmpty() && st.valuesDepth.peek() > st.depth) st.valuesDepth.pop();

        while (!st.caseStack.isEmpty() && st.caseStack.peek().parenDepthAtStart > st.depth) st.caseStack.pop();

        st.i++;
    }

    /**
     * ✅ Leading comma style for top-level list contexts:
     * - SELECT list
     * - SET list
     * - INTO column list
     * - VALUES list
     * - WITH CTE list
     */
    private static void handleComma(State st) {
        // consume ','
        st.i++;
        int j = st.i;
        while (j < st.n && Character.isWhitespace(st.s.charAt(j))) j++;

        // WITH cte1 AS (...), cte2 AS (...)
        if (st.inWith && st.depth == st.withBaseDepth) {
            emitLeadingComma(st, ind(st.depth, st.activeOffset), j);
            return;
        }

        // SELECT list
        if (!st.selectListDepth.isEmpty() && st.depth == st.selectListDepth.peek()) {
            emitLeadingComma(st, ind(st.depth, st.activeOffset) + 1, j);
            return;
        }

        // UPDATE SET list
        if (!st.setDepth.isEmpty() && st.depth == st.setDepth.peek()) {
            emitLeadingComma(st, ind(st.depth, st.activeOffset) + 1, j);
            return;
        }

        // INSERT INTO (col list)
        if (!st.intoColsDepth.isEmpty() && st.depth == st.intoColsDepth.peek()) {
            emitLeadingComma(st, ind(st.depth, st.activeOffset), j);
            return;
        }

        // VALUES (value list)
        if (!st.valuesDepth.isEmpty() && st.depth == st.valuesDepth.peek()) {
            emitLeadingComma(st, ind(st.depth, st.activeOffset), j);
            return;
        }

        // default comma (non-list)
        st.out.append(',');

        // function args list (multiline mode)
        if (OPT_FUNC_ARGS && !st.funcStack.isEmpty() && st.funcStack.peek().parenDepth == st.depth) {

            // compact last simple literal: "... END, 0 )"
            if (OPT_FUNC_ARGS_COMPACT_LAST_LITERAL) {
                int p = skipWsOnly(st.s, j, st.n);
                Consumed lit = consumeSimpleLiteral(st.s, p, st.n);
                if (lit != null) {
                    int q = skipWsAndComments(st.s, lit.nextIndex, st.n);
                    if (q < st.n && st.s.charAt(q) == ')') {
                        st.out.append(' ');
                        st.i = j;
                        return;
                    }
                }
            }

            st.out.append('\n');
            appendIndent(st.out, st.funcStack.peek().argIndent);
            st.i = j;
            return;
        }

        // parentheses: if next is CASE/SELECT/WITH => newline+indent
        if (st.depth > 0) {
            WordHit next = readWordUpperAt(st.s, j, st.n);
            if (next != null && (next.word.equals("CASE") || next.word.equals("SELECT") || next.word.equals("WITH"))) {
                st.out.append('\n');
                appendIndent(st.out, ind(st.depth, st.activeOffset));
                st.i = j;
                return;
            }
        }

        if (j < st.n) {
            char nx = st.s.charAt(j);
            if (nx != ')' && nx != ',' && nx != ';' && nx != '\n') st.out.append(' ');
        }
        st.i = j;
    }

    private static void emitLeadingComma(State st, int indentLevel, int nextIndex) {
        if (st.out.length() == 0) {
            st.out.append(", ");
        } else {
            ensureLineStart(st.out, indentLevel);
            st.out.append(", ");
        }
        st.i = nextIndex; // ",\nCOL" => ", COL"
    }

    private static void handleWordToken(State st) {
        int start = st.i;
        st.i++;
        while (st.i < st.n && isWordPart(st.s.charAt(st.i))) st.i++;

        String raw = st.s.substring(start, st.i);
        String up = raw.toUpperCase(Locale.ROOT);

        // MERGE context (best-effort)
        if (up.equals("MERGE")) st.inMerge = true;

        // end condition context when new clause starts at same depth
        if (isCondTerminatorKeywordForThisContext(st, up)) {
            endConditionAtDepth(st.condDepth, st.inlineCondDepth, st.depth);
        }

        // JOIN modifiers
        if (JOIN_MODIFIERS.contains(up)) {
            int p = skipWsOnly(st.s, st.i, st.n);
            WordHit w1 = readWordUpperAt(st.s, p, st.n);

            if (w1 != null) {
                if ("OUTER".equals(w1.word) && (up.equals("LEFT") || up.equals("RIGHT") || up.equals("FULL"))) {
                    int p2 = skipWsOnly(st.s, w1.end, st.n);
                    WordHit w2 = readWordUpperAt(st.s, p2, st.n);
                    if (w2 != null && "JOIN".equals(w2.word)) {
                        ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                        st.out.append(up).append(" OUTER JOIN ");
                        st.i = w2.end;
                        return;
                    }
                }
                if ("JOIN".equals(w1.word)) {
                    ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                    st.out.append(up).append(" JOIN ");
                    st.i = w1.end;
                    return;
                }
            }
        }
        if ("JOIN".equals(up)) {
            ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
            st.out.append("JOIN ");
            return;
        }

        // GROUP BY / ORDER BY
        if (up.equals("GROUP") || up.equals("ORDER")) {
            int j = skipWsOnly(st.s, st.i, st.n);
            WordHit next = readWordUpperAt(st.s, j, st.n);
            if (next != null && "BY".equals(next.word)) {
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                st.out.append(up).append(" BY ");
                st.i = next.end;
                return;
            }
        }

        // CASE formatting (entry)
        if (up.equals("CASE")) {
            int baseIndent = computeCaseBaseIndent(
                    st.depth, st.activeOffset,
                    st.selectListDepth, st.setDepth, st.condDepth, st.valuesDepth,
                    st.funcStack
            );
            baseIndent = Math.max(baseIndent, currentLineIndentLevel(st.out));

            ensureLineStart(st.out, baseIndent);
            st.out.append("CASE");

            st.caseStack.push(new CaseCtx(st.depth, baseIndent));

            int j = skipWsOnly(st.s, st.i, st.n);
            char nx = peekNonWsChar(st.s, j, st.n);
            if (nx != '\0' && nx != ',' && nx != ')' && nx != ';') st.out.append(' ');
            st.i = j;
            return;
        }

        // CASE keywords
        if (!st.caseStack.isEmpty() && (up.equals("WHEN") || up.equals("THEN") || up.equals("ELSE") || up.equals("END"))) {
            handleCaseKeyword(st, up);
            return;
        }

        // Clause keywords
        if (CLAUSE_KEYWORDS.contains(up)) {
            handleClauseKeyword(st, up);
            return;
        }

        // AND/OR inside condition clause
        if ((up.equals("AND") || up.equals("OR")) && !st.condDepth.isEmpty() && st.depth >= st.condDepth.peek()) {
            // inline-cond => keep inline
            if (isInlineCondActive(st.inlineCondDepth, st.depth)) {
                ensureSingleSpace(st.out);
                st.out.append(up).append(' ');
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;
            }

            ensureLineStart(st.out, ind(st.depth, st.activeOffset) + 1);
            st.out.append(up).append(' ');
            st.i = skipWsOnly(st.s, st.i, st.n);
            return;
        }

        // DISTINCT/ALL immediately after SELECT
        if ((up.equals("DISTINCT") || up.equals("ALL")) && !st.selectListDepth.isEmpty() && st.selectListDepth.peek() == st.depth) {
            st.out.append(' ').append(up);
            st.pendingSelectFirstItemNewline = true;
            st.pendingSelectIndent = ind(st.depth, st.activeOffset) + 1;
            st.i = skipWsOnly(st.s, st.i, st.n);
            return;
        }

        st.out.append(raw);
    }

    private static void handleCaseKeyword(State st, String up) {
        CaseCtx ctx = st.caseStack.peek();

        if (up.equals("END")) {
            ctx.whenLineActive = false;
            ensureLineStart(st.out, ctx.baseIndent);
            st.out.append("END");
            st.caseStack.pop();

            int j = skipWsOnly(st.s, st.i, st.n);
            char nx = peekNonWsChar(st.s, j, st.n);
            if (nx != '\0' && nx != ',' && nx != ')' && nx != ';') st.out.append(' ');
            st.i = j;
            return;
        }

        if (up.equals("WHEN")) {
            if (OPT_CASE_BLANK_LINE_BETWEEN_BRANCHES && ctx.seenBranch) {
                ensureBlankLineStart(st.out, ctx.innerIndent);
            } else {
                ensureLineStart(st.out, ctx.innerIndent);
            }

            ctx.seenBranch = true;
            ctx.whenLineActive = true;

            st.out.append("WHEN ");
            st.i = skipWsOnly(st.s, st.i, st.n);
            return;
        }

        if (up.equals("THEN")) {
            // inline ONLY small numeric literal
            if (OPT_CASE_INLINE_SIMPLE && ctx.whenLineActive) {
                int p = skipWsOnly(st.s, st.i, st.n);
                Consumed lit = consumeSimpleLiteral(st.s, p, st.n);
                if (lit != null
                        && isCaseBoundaryAfterLiteral(st.s, lit.nextIndex, st.n)
                        && isInlineableSmallNumber(lit.text)) {
                    ensureSingleSpace(st.out);
                    st.out.append("THEN ").append(lit.text);
                    ctx.whenLineActive = false;
                    st.i = skipWsOnly(st.s, lit.nextIndex, st.n);
                    return;
                }
            }

            // requested style: WHEN ... THEN (same line), expression on next line
            if (OPT_CASE_WHEN_THEN_SAME_LINE && ctx.whenLineActive) {
                ensureSingleSpace(st.out);
                st.out.append("THEN");
                st.out.append('\n');
                appendIndent(st.out, ctx.innerIndent + 1);
                ctx.whenLineActive = false;
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;
            }

            ctx.whenLineActive = false;

            // fallback behavior
            if (OPT_CASE_THEN_NEWLINE) {
                ensureLineStart(st.out, ctx.innerIndent);
                st.out.append("THEN");
                st.out.append('\n');
                appendIndent(st.out, ctx.innerIndent + 1);
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;
            }

            ensureLineStart(st.out, ctx.innerIndent);
            st.out.append("THEN ");
            st.i = skipWsOnly(st.s, st.i, st.n);
            return;
        }

        if (up.equals("ELSE")) {
            ctx.whenLineActive = false;

            if (OPT_CASE_BLANK_LINE_BETWEEN_BRANCHES && ctx.seenBranch) {
                ensureBlankLineStart(st.out, ctx.innerIndent);
            } else {
                ensureLineStart(st.out, ctx.innerIndent);
            }
            ctx.seenBranch = true;

            // inline ONLY small numeric literal
            if (OPT_CASE_INLINE_SIMPLE) {
                int p = skipWsOnly(st.s, st.i, st.n);
                Consumed lit = consumeSimpleLiteral(st.s, p, st.n);
                if (lit != null
                        && isCaseBoundaryAfterLiteral(st.s, lit.nextIndex, st.n)
                        && isInlineableSmallNumber(lit.text)) {
                    st.out.append("ELSE ").append(lit.text);
                    st.i = skipWsOnly(st.s, lit.nextIndex, st.n);
                    return;
                }
            }

            if (OPT_CASE_ELSE_NEWLINE) {
                st.out.append("ELSE");
                st.out.append('\n');
                appendIndent(st.out, ctx.innerIndent + 1);
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;
            }

            st.out.append("ELSE ");
            st.i = skipWsOnly(st.s, st.i, st.n);
        }
    }

    private static void handleClauseKeyword(State st, String up) {
        switch (up) {
            case "WITH":
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                st.out.append("WITH ");
                st.inWith = true;
                st.withBaseDepth = st.depth;
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;

            case "SELECT":
                if (st.inWith && st.depth == st.withBaseDepth) {
                    st.inWith = false;
                }

                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                st.out.append("SELECT");

                st.selectListDepth.push(st.depth);
                st.pendingSelectFirstItemNewline = true;
                st.pendingSelectIndent = ind(st.depth, st.activeOffset) + 1;

                st.i = skipWsOnly(st.s, st.i, st.n);
                return;

            case "FROM":
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                st.out.append("FROM ");
                if (!st.selectListDepth.isEmpty() && st.selectListDepth.peek() == st.depth) st.selectListDepth.pop();
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;

            case "WHERE":
            case "HAVING": {
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));

                boolean inline = OPT_INLINE_SINGLE_COND_WHERE
                        && isInlineSingleConditionClause(st.s, st.i, st.n, st.depth, OPT_MAX_LINE_LEN);

                if (inline) {
                    st.out.append(up).append(' ');
                    st.condDepth.push(st.depth);
                    st.inlineCondDepth.push(st.depth);
                    st.i = skipWsOnly(st.s, st.i, st.n);
                    return;
                }

                st.out.append(up).append("\n");
                appendIndent(st.out, ind(st.depth, st.activeOffset) + 1);
                st.condDepth.push(st.depth);
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;
            }

            case "ON": {
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));

                boolean inline = OPT_INLINE_SINGLE_COND_WHERE
                        && isInlineSingleConditionClause(st.s, st.i, st.n, st.depth, OPT_MAX_LINE_LEN);

                if (inline) {
                    st.out.append("ON ");
                    st.condDepth.push(st.depth);
                    st.inlineCondDepth.push(st.depth);
                    st.i = skipWsOnly(st.s, st.i, st.n);
                    return;
                }

                st.out.append("ON").append("\n");
                appendIndent(st.out, ind(st.depth, st.activeOffset) + 1);
                st.condDepth.push(st.depth);
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;
            }

            case "SET":
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                st.out.append("SET").append("\n");
                appendIndent(st.out, ind(st.depth, st.activeOffset) + 1);
                st.setDepth.push(st.depth);
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;

            case "INTO":
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                st.out.append("INTO ");
                st.afterInto = true;
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;

            case "VALUES":
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                st.out.append("VALUES ");
                st.afterValues = true;
                st.i = skipWsOnly(st.s, st.i, st.n);
                return;

            default:
                ensureClauseStart(st.out, ind(st.depth, st.activeOffset));
                st.out.append(up).append(' ');
                st.i = skipWsOnly(st.s, st.i, st.n);
        }
    }

    // ========================= Condition terminator (MERGE-aware) =========================

    private static boolean isCondTerminatorKeywordForThisContext(State st, String up) {
        // 기본 terminator
        if (isCondTerminatorKeyword(up)) return true;

        // MERGE의 ON (...) 다음 WHEN 처리 (CASE 내부는 제외)
        if (st.inMerge && st.caseStack.isEmpty() && up.equals("WHEN")) return true;

        return false;
    }

    private static boolean isCondTerminatorKeyword(String up) {
        // starts a new clause that ends WHERE/ON/HAVING at the same depth
        return up.equals("FROM")
                || up.equals("WHERE")
                || up.equals("GROUP")
                || up.equals("ORDER")
                || up.equals("HAVING")
                || up.equals("UNION")
                || up.equals("INTERSECT")
                || up.equals("MINUS")
                || up.equals("EXCEPT")
                || up.equals("JOIN")
                || up.equals("USING")
                || up.equals("SET")
                || up.equals("VALUES")
                || up.equals("INTO")
                || up.equals("MERGE");
    }

    // ========================= Helpers =========================

    private static int ind(int depth, int activeOffset) {
        return Math.max(0, depth + activeOffset);
    }

    private static int computeCaseBaseIndent(int depth,
                                             int activeOffset,
                                             Deque<Integer> selectListDepth,
                                             Deque<Integer> setDepth,
                                             Deque<Integer> condDepth,
                                             Deque<Integer> valuesDepth,
                                             Deque<FuncCtx> funcStack) {
        if (!selectListDepth.isEmpty() && selectListDepth.peek() == depth) return ind(depth, activeOffset) + 1;
        if (!setDepth.isEmpty() && setDepth.peek() == depth) return ind(depth, activeOffset) + 1;
        if (!condDepth.isEmpty() && condDepth.peek() <= depth) return ind(depth, activeOffset) + 1;
        if (!valuesDepth.isEmpty() && valuesDepth.peek() == depth) return ind(depth, activeOffset);
        if (!funcStack.isEmpty() && funcStack.peek().parenDepth == depth) return ind(depth, activeOffset);
        return ind(depth, activeOffset);
    }

    private static void ensureClauseStart(StringBuilder out, int indentLevel) {
        ensureLineStart(out, indentLevel);
    }

    private static void ensureLineStart(StringBuilder out, int indentLevel) {
        rtrimSpaces(out);
        if (out.length() == 0) return;

        if (!endsWithNewline(out)) out.append('\n');
        appendIndent(out, indentLevel);
    }

    private static void ensureBlankLineStart(StringBuilder out, int indentLevel) {
        rtrimSpaces(out);
        if (out.length() == 0) return;

        if (!endsWithNewline(out)) out.append('\n');

        int len = out.length();
        boolean alreadyBlank = (len >= 2 && out.charAt(len - 1) == '\n' && out.charAt(len - 2) == '\n');
        if (!alreadyBlank) out.append('\n');

        appendIndent(out, indentLevel);
    }

    private static void appendIndent(StringBuilder out, int indentLevel) {
        int lvl = Math.max(0, indentLevel);
        for (int i = 0; i < lvl; i++) out.append(IND);
    }

    private static boolean endsWithNewline(StringBuilder out) {
        int len = out.length();
        return len == 0 || out.charAt(len - 1) == '\n';
    }

    private static void rtrimSpaces(StringBuilder out) {
        int len = out.length();
        while (len > 0) {
            char c = out.charAt(len - 1);
            if (c == ' ' || c == '\t' || c == '\r') {
                out.setLength(len - 1);
                len--;
            } else break;
        }
    }

    private static int skipWsOnly(String s, int i, int n) {
        int p = i;
        while (p < n) {
            char c = s.charAt(p);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') { p++; continue; }
            break;
        }
        return p;
    }

    // skip spaces/tabs/CR only (keep newline)
    private static int skipSpacesTabsCrOnly(String s, int i, int n) {
        int p = i;
        while (p < n) {
            char c = s.charAt(p);
            if (c == ' ' || c == '\t' || c == '\r') { p++; continue; }
            break;
        }
        return p;
    }

    private static char peekNonWsChar(String s, int i, int n) {
        int p = skipWsOnly(s, i, n);
        return (p < n) ? s.charAt(p) : '\0';
    }

    private static boolean startsWithKeywordAfterParen(String s, int i, int n, String keywordUpper) {
        int p = i;

        while (p < n) {
            while (p < n && Character.isWhitespace(s.charAt(p))) p++;

            if (p + 1 < n && s.charAt(p) == '-' && s.charAt(p + 1) == '-') {
                p += 2;
                while (p < n && s.charAt(p) != '\n') p++;
                continue;
            }
            if (p + 1 < n && s.charAt(p) == '/' && s.charAt(p + 1) == '*') {
                p += 2;
                while (p + 1 < n) {
                    if (s.charAt(p) == '*' && s.charAt(p + 1) == '/') { p += 2; break; }
                    p++;
                }
                continue;
            }
            break;
        }

        int klen = keywordUpper.length();
        if (p + klen > n) return false;

        for (int k = 0; k < klen; k++) {
            if (Character.toUpperCase(s.charAt(p + k)) != keywordUpper.charAt(k)) return false;
        }

        int after = p + klen;
        if (after < n && isWordPart(s.charAt(after))) return false;
        return true;
    }

    private static String prevWordUpper(String s, int openPos) {
        int p = openPos - 1;
        while (p >= 0 && Character.isWhitespace(s.charAt(p))) p--;
        if (p < 0) return null;
        char c = s.charAt(p);
        if (!isWordPart(c)) return null;

        int end = p + 1;
        int start = p;
        while (start - 1 >= 0 && isWordPart(s.charAt(start - 1))) start--;
        return s.substring(start, end).toUpperCase(Locale.ROOT);
    }

    private static boolean isWordStart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }

    private static boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }

    private static final class WordHit {
        final String word; // UPPER
        final int end;
        WordHit(String word, int end) { this.word = word; this.end = end; }
    }

    private static WordHit readWordUpperAt(String s, int pos, int n) {
        int p = pos;
        if (p >= n) return null;

        // MyBatis 바인딩은 word로 취급하지 않음
        if (p + 1 < n && ((s.charAt(p) == '#' || s.charAt(p) == '$') && s.charAt(p + 1) == '{')) return null;

        if (!isWordStart(s.charAt(p))) return null;

        int q = p + 1;
        while (q < n && isWordPart(s.charAt(q))) q++;
        return new WordHit(s.substring(p, q).toUpperCase(Locale.ROOT), q);
    }

    // ====== helpers for maxLineLen / indent ======

    private static int currentLineIndentLevel(StringBuilder out) {
        int len = out.length();
        int p = len - 1;

        while (p >= 0 && out.charAt(p) != '\n') p--;
        int start = p + 1;

        int spaces = 0;
        for (int i = start; i < len; i++) {
            char c = out.charAt(i);
            if (c == ' ') spaces++;
            else if (c == '\t') spaces += 4;
            else break;
        }
        return spaces / 4;
    }

    private static boolean isFunctionCallShortEnough(String s, int openParenPos, int afterOpenPos, int n, int depthAfterOpen, int maxLen) {
        int start = findFunctionNameStart(s, openParenPos);
        int end = findMatchingParenPos(s, afterOpenPos, n, depthAfterOpen);
        if (start < 0 || end < 0) return false;

        int span = end - start + 1;
        if (span <= 0) return false;

        for (int i = start; i <= end && i < n; i++) {
            if (s.charAt(i) == '\n') return false;
        }

        return span <= Math.max(40, maxLen);
    }

    private static int findFunctionNameStart(String s, int openParenPos) {
        int p = openParenPos - 1;
        if (p < 0) return -1;

        while (p >= 0 && Character.isWhitespace(s.charAt(p))) p--;
        if (p < 0) return -1;

        int end = p + 1;
        int start = p;
        while (start - 1 >= 0) {
            char c = s.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#' || c == '.' ) start--;
            else break;
        }
        if (start >= end) return -1;
        return start;
    }

    private static int findMatchingParenPos(String s, int pos, int n, int depthAfterOpen) {
        int depth = depthAfterOpen;

        boolean inStr = false;
        boolean inLine = false;
        boolean inBlock = false;

        int i = pos;
        while (i < n) {
            char c = s.charAt(i);
            char nx = (i + 1 < n) ? s.charAt(i + 1) : '\0';

            if (inLine) {
                if (c == '\n') inLine = false;
                i++;
                continue;
            }
            if (inBlock) {
                if (c == '*' && nx == '/') { inBlock = false; i += 2; continue; }
                i++;
                continue;
            }
            if (inStr) {
                if (c == '\'') {
                    if (nx == '\'') { i += 2; continue; }
                    inStr = false;
                    i++;
                    continue;
                }
                i++;
                continue;
            }

            if (c == '-' && nx == '-') { inLine = true; i += 2; continue; }
            if (c == '/' && nx == '*') { inBlock = true; i += 2; continue; }
            if (c == '\'') { inStr = true; i++; continue; }

            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == depthAfterOpen - 1) return i;
            }
            i++;
        }
        return -1;
    }

    private static boolean containsKeywordInParenBody(String s, int afterOpenPos, int n, String kwUpper, int depthAfterOpen) {
        int end = findMatchingParenPos(s, afterOpenPos, n, depthAfterOpen);
        if (end < 0) return false;

        String kw = kwUpper;
        int k = kw.length();

        for (int i = afterOpenPos; i + k <= end; i++) {
            char c = s.charAt(i);
            if (!Character.isLetter(c)) continue;

            if (i > afterOpenPos && isWordPart(s.charAt(i - 1))) continue;

            boolean ok = true;
            for (int j = 0; j < k; j++) {
                if (Character.toUpperCase(s.charAt(i + j)) != kw.charAt(j)) { ok = false; break; }
            }
            if (!ok) continue;

            int after = i + k;
            if (after <= end && after < n && isWordPart(s.charAt(after))) continue;

            return true;
        }
        return false;
    }

    // ========================= PATCH helpers =========================

    private static void ensureSingleSpace(StringBuilder out) {
        int len = out.length();
        if (len == 0) return;
        char last = out.charAt(len - 1);
        if (last != ' ' && last != '\t' && last != '\n' && last != '\r') out.append(' ');
    }

    private static final class Consumed {
        final String text;
        final int nextIndex;
        Consumed(String text, int nextIndex) { this.text = text; this.nextIndex = nextIndex; }
    }

    private static Consumed consumeSimpleLiteral(String s, int i, int n) {
        int p = skipWsOnly(s, i, n);
        if (p >= n) return null;

        // MyBatis bindings are not literals here
        if (p + 1 < n && ((s.charAt(p) == '#' || s.charAt(p) == '$') && s.charAt(p + 1) == '{')) return null;

        char c = s.charAt(p);

        // string literal
        if (c == '\'') {
            int start = p;
            p++;
            while (p < n) {
                char x = s.charAt(p++);
                if (x == '\'') {
                    if (p < n && s.charAt(p) == '\'') { p++; continue; } // escaped ''
                    break;
                }
            }
            return new Consumed(s.substring(start, p), p);
        }

        // number literal
        if (c == '-' || Character.isDigit(c)) {
            int start = p;
            if (c == '-') p++;
            int digits = 0;
            while (p < n && Character.isDigit(s.charAt(p))) { p++; digits++; }
            if (p < n && s.charAt(p) == '.') {
                p++;
                while (p < n && Character.isDigit(s.charAt(p))) { p++; digits++; }
            }
            if (digits == 0) return null;
            return new Consumed(s.substring(start, p), p);
        }

        // identifier (single word) — not function call
        if (isWordStart(c) && c != '#') {
            int start = p;
            p++;
            while (p < n && isWordPart(s.charAt(p))) p++;
            int q = skipWsOnly(s, p, n);
            if (q < n && s.charAt(q) == '(') return null; // function call
            return new Consumed(s.substring(start, p), p);
        }

        return null;
    }

    private static int skipWsAndComments(String s, int i, int n) {
        int p = i;
        while (p < n) {
            while (p < n && Character.isWhitespace(s.charAt(p))) p++;

            if (p + 1 < n && s.charAt(p) == '-' && s.charAt(p + 1) == '-') {
                p += 2;
                while (p < n && s.charAt(p) != '\n') p++;
                continue;
            }
            if (p + 1 < n && s.charAt(p) == '/' && s.charAt(p + 1) == '*') {
                p += 2;
                while (p + 1 < n) {
                    if (s.charAt(p) == '*' && s.charAt(p + 1) == '/') { p += 2; break; }
                    p++;
                }
                continue;
            }
            break;
        }
        return p;
    }

    private static boolean isCaseBoundaryAfterLiteral(String s, int nextIndex, int n) {
        int p = skipWsAndComments(s, nextIndex, n);
        if (p >= n) return true;

        char c = s.charAt(p);
        if (c == ',' || c == ')' || c == ';') return true;

        WordHit w = readWordUpperAt(s, p, n);
        if (w == null) return false;

        return w.word.equals("WHEN") || w.word.equals("ELSE") || w.word.equals("END");
    }

    // only inline small integers: "4", "3", "10"
    private static boolean isInlineableSmallNumber(String text) {
        if (text == null) return false;
        int p = 0;
        int n = text.length();
        while (p < n && Character.isWhitespace(text.charAt(p))) p++;
        if (p >= n) return false;

        boolean neg = false;
        if (text.charAt(p) == '-') { neg = true; p++; }
        if (p >= n || !Character.isDigit(text.charAt(p))) return false;

        int digits = 0;
        while (p < n && Character.isDigit(text.charAt(p))) { p++; digits++; }

        // disallow decimals for inline
        if (p < n && text.charAt(p) == '.') return false;

        // skip trailing spaces
        while (p < n && Character.isWhitespace(text.charAt(p))) p++;

        if (p != n) return false;

        // only 1~2 digits
        return digits >= 1 && digits <= 2 && !(neg && digits > 2);
    }

    // ===== comment indent helpers =====

    private static int lastNewlinePos(StringBuilder out) {
        for (int i = out.length() - 1; i >= 0; i--) {
            if (out.charAt(i) == '\n') return i;
        }
        return -1;
    }

    private static boolean isOnlyIndentSinceNewline(StringBuilder out) {
        int start = lastNewlinePos(out) + 1;
        for (int i = start; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c != ' ' && c != '\t') return false;
        }
        return true;
    }

    private static int preferredIndentLevel(State st) {
        if (!st.caseStack.isEmpty()) return st.caseStack.peek().innerIndent;

        if (!st.funcStack.isEmpty() && st.funcStack.peek().parenDepth == st.depth) return st.funcStack.peek().argIndent;

        if (!st.condDepth.isEmpty() && st.depth >= st.condDepth.peek()) return ind(st.depth, st.activeOffset) + 1;

        if (!st.selectListDepth.isEmpty() && st.selectListDepth.peek() == st.depth) return ind(st.depth, st.activeOffset) + 1;

        if (!st.setDepth.isEmpty() && st.setDepth.peek() == st.depth) return ind(st.depth, st.activeOffset) + 1;

        if (!st.valuesDepth.isEmpty() && st.valuesDepth.peek() == st.depth) return ind(st.depth, st.activeOffset);

        if (!st.intoColsDepth.isEmpty() && st.intoColsDepth.peek() == st.depth) return ind(st.depth, st.activeOffset);

        if (st.inWith && st.depth == st.withBaseDepth) return ind(st.depth, st.activeOffset);

        return ind(st.depth, st.activeOffset);
    }

    // ========================= inline WHERE detection =========================

    private static boolean isInlineCondActive(Deque<Integer> inlineCondDepth, int depth) {
        return !inlineCondDepth.isEmpty() && depth >= inlineCondDepth.peek();
    }

    private static void endConditionAtDepth(Deque<Integer> condDepth, Deque<Integer> inlineCondDepth, int depth) {
        while (!condDepth.isEmpty() && condDepth.peek() == depth) condDepth.pop();
        while (!inlineCondDepth.isEmpty() && inlineCondDepth.peek() == depth) inlineCondDepth.pop();
    }

    private static boolean isInlineSingleConditionClause(String s, int fromIndex, int n, int baseDepth, int maxLen) {
        int start = skipWsOnly(s, fromIndex, n);
        if (start >= n) return false;

        int depth = baseDepth;
        boolean inStr = false, inLine = false, inBlock = false;
        boolean betweenArmed = false;

        int i = start;

        while (i < n) {
            char c = s.charAt(i);
            char nx = (i + 1 < n) ? s.charAt(i + 1) : '\0';

            if (c == '\n') return false; // keep single-line only

            if (inLine) {
                if (c == '\n') inLine = false;
                i++;
                continue;
            }
            if (inBlock) {
                if (c == '*' && nx == '/') { inBlock = false; i += 2; continue; }
                i++;
                continue;
            }
            if (inStr) {
                if (c == '\'') {
                    if (nx == '\'') { i += 2; continue; }
                    inStr = false;
                    i++;
                    continue;
                }
                i++;
                continue;
            }

            if (c == '-' && nx == '-') { inLine = true; i += 2; continue; }
            if (c == '/' && nx == '*') { inBlock = true; i += 2; continue; }
            if (c == '\'') { inStr = true; i++; continue; }

            // MyBatis binding skip
            if ((c == '#' || c == '$') && nx == '{') {
                i += 2;
                while (i < n) {
                    if (s.charAt(i++) == '}') break;
                }
                continue;
            }

            if (c == '(') { depth++; i++; continue; }
            if (c == ')') {
                depth = Math.max(0, depth - 1);
                i++;
                continue;
            }
            if (c == ';') break;

            if (isWordStart(c)) {
                int wStart = i;
                i++;
                while (i < n && isWordPart(s.charAt(i))) i++;
                String w = s.substring(wStart, i).toUpperCase(Locale.ROOT);

                // clause boundary at base depth
                if (depth == baseDepth && isCondTerminatorKeyword(w)) break;

                if (w.equals("SELECT") || w.equals("CASE") || w.equals("WITH")) return false;

                if (depth == baseDepth && w.equals("BETWEEN")) {
                    betweenArmed = true;
                    continue;
                }

                if (depth == baseDepth && (w.equals("AND") || w.equals("OR"))) {
                    if (betweenArmed && w.equals("AND")) {
                        betweenArmed = false; // BETWEEN's AND
                        continue;
                    }
                    return false; // real logical AND/OR => not single cond
                }

                continue;
            }

            i++;
        }

        int end = i;
        int span = end - start;
        if (span <= 0) return false;

        return span <= Math.max(20, maxLen);
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}

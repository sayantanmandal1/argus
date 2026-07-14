package io.argus.query;

import io.argus.analysis.Analyzer;
import io.argus.search.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A recursive-descent parser for a Lucene-style query DSL, turning a user string into a {@link Query}.
 *
 * <p>Supported syntax:
 * <ul>
 *   <li>{@code term} — a word in the default field (analyzed with the same analyzer as the index)</li>
 *   <li>{@code field:term} — a word in a specific field</li>
 *   <li>{@code "a b c"} — an exact phrase</li>
 *   <li>{@code prefix*} — a prefix (wildcard) query</li>
 *   <li>{@code AND OR NOT} (or {@code && || !}) and {@code +required} / {@code -excluded}</li>
 *   <li>{@code ( ... )} grouping, and {@code field:( ... )} to apply a field to a group</li>
 * </ul>
 *
 * <p>Query terms are run through the analyzer so a search for {@code Running} matches the indexed
 * stem {@code run}. The default operator between adjacent terms is configurable (OR or AND).
 */
public final class QueryParser {

    /** The operator applied between adjacent clauses that have no explicit operator. */
    public enum Operator {
        AND,
        OR
    }

    private static final int CONJ_NONE = 0;
    private static final int CONJ_AND = 1;
    private static final int CONJ_OR = 2;

    private static final int MOD_NONE = 0;
    private static final int MOD_REQUIRED = 1;
    private static final int MOD_PROHIBITED = 2;

    private final String defaultField;
    private final Analyzer analyzer;
    private Operator defaultOperator = Operator.OR;

    private String input = "";
    private int pos;

    public QueryParser(String defaultField, Analyzer analyzer) {
        this.defaultField = Objects.requireNonNull(defaultField, "defaultField");
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
    }

    public QueryParser defaultOperator(Operator operator) {
        this.defaultOperator = Objects.requireNonNull(operator, "operator");
        return this;
    }

    public Operator defaultOperator() {
        return defaultOperator;
    }

    /** Parses {@code queryString} into a {@link Query}. */
    public Query parse(String queryString) {
        this.input = queryString == null ? "" : queryString;
        this.pos = 0;
        Query query = parseBoolean(defaultField, false);
        skipWhitespace();
        if (!eof()) {
            throw new QueryParseException("unexpected '" + peek() + "' at position " + pos);
        }
        return query;
    }

    private Query parseBoolean(String field, boolean inParentheses) {
        List<BooleanClause> clauses = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (eof() || (inParentheses && peek() == ')')) {
                break;
            }
            int loopStart = pos;

            int conjunction = CONJ_NONE;
            if (matchKeyword("AND") || matchLiteral("&&")) {
                conjunction = CONJ_AND;
            } else if (matchKeyword("OR") || matchLiteral("||")) {
                conjunction = CONJ_OR;
            }

            skipWhitespace();
            int modifier = MOD_NONE;
            if (match('+')) {
                modifier = MOD_REQUIRED;
            } else if (match('-') || match('!') || matchKeyword("NOT")) {
                modifier = MOD_PROHIBITED;
            }

            skipWhitespace();
            if (eof() || (inParentheses && peek() == ')')) {
                break;
            }

            Query clause = parseClause(field);
            if (clause != null) {
                addClause(clauses, conjunction, modifier, clause);
            }
            if (pos == loopStart) {
                pos++; // guarantee forward progress on stray input
            }
        }
        return buildBoolean(clauses);
    }

    private Query parseClause(String defaultFieldForClause) {
        skipWhitespace();
        String field = defaultFieldForClause;

        int save = pos;
        String ident = readIdent();
        if (ident != null && !eof() && peek() == ':') {
            pos++; // consume ':'
            field = ident;
        } else {
            pos = save;
        }

        skipWhitespace();
        if (!eof() && peek() == '(') {
            pos++;
            Query sub = parseBoolean(field, true);
            skipWhitespace();
            if (eof() || peek() != ')') {
                throw new QueryParseException("expected ')' at position " + pos);
            }
            pos++;
            return sub;
        }
        if (!eof() && peek() == '"') {
            return parsePhrase(field);
        }
        return parseTerm(field);
    }

    private Query parsePhrase(String field) {
        pos++; // opening quote
        int start = pos;
        while (!eof() && peek() != '"') {
            pos++;
        }
        if (eof()) {
            throw new QueryParseException("unterminated phrase starting at position " + (start - 1));
        }
        String text = input.substring(start, pos);
        pos++; // closing quote
        List<String> terms = analyzer.terms(field, text);
        if (terms.isEmpty()) {
            return null;
        }
        if (terms.size() == 1) {
            return new TermQuery(field, terms.get(0));
        }
        return new PhraseQuery(field, terms);
    }

    private Query parseTerm(String field) {
        int start = pos;
        while (!eof()) {
            char c = peek();
            if (Character.isLetterOrDigit(c) || c == '*') {
                pos++;
            } else {
                break;
            }
        }
        String raw = input.substring(start, pos);
        if (raw.isEmpty()) {
            return null;
        }
        if (raw.endsWith("*")) {
            String prefix = raw.substring(0, raw.length() - 1).toLowerCase(Locale.ROOT);
            return prefix.isEmpty() ? null : new PrefixQuery(field, prefix);
        }
        List<String> terms = analyzer.terms(field, raw);
        if (terms.isEmpty()) {
            return null;
        }
        if (terms.size() == 1) {
            return new TermQuery(field, terms.get(0));
        }
        return new PhraseQuery(field, terms);
    }

    private void addClause(List<BooleanClause> clauses, int conjunction, int modifier, Query query) {
        boolean prohibited = modifier == MOD_PROHIBITED;
        boolean required = modifier == MOD_REQUIRED;

        boolean andWithPrevious = conjunction == CONJ_AND
                || (conjunction == CONJ_NONE && defaultOperator == Operator.AND);
        if (andWithPrevious) {
            if (!prohibited) {
                required = true;
            }
            upgradePreviousToRequired(clauses);
        }

        Occur occur = prohibited ? Occur.MUST_NOT : (required ? Occur.MUST : Occur.SHOULD);
        clauses.add(new BooleanClause(query, occur));
    }

    private void upgradePreviousToRequired(List<BooleanClause> clauses) {
        if (clauses.isEmpty()) {
            return;
        }
        BooleanClause last = clauses.get(clauses.size() - 1);
        if (last.occur() == Occur.SHOULD) {
            clauses.set(clauses.size() - 1, new BooleanClause(last.query(), Occur.MUST));
        }
    }

    private Query buildBoolean(List<BooleanClause> clauses) {
        if (clauses.isEmpty()) {
            return new BooleanQuery.Builder().build();
        }
        if (clauses.size() == 1 && clauses.get(0).occur() == Occur.SHOULD) {
            return clauses.get(0).query();
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (BooleanClause c : clauses) {
            builder.add(c.query(), c.occur());
        }
        return builder.build();
    }

    // --------------------------------------------------------------------- lexer helpers

    private String readIdent() {
        int start = pos;
        while (!eof()) {
            char c = peek();
            if (Character.isLetterOrDigit(c) || c == '_') {
                pos++;
            } else {
                break;
            }
        }
        return pos > start ? input.substring(start, pos) : null;
    }

    private boolean matchKeyword(String keyword) {
        if (!input.regionMatches(pos, keyword, 0, keyword.length())) {
            return false;
        }
        int after = pos + keyword.length();
        if (after < input.length()) {
            char c = input.charAt(after);
            if (Character.isLetterOrDigit(c) || c == '*' || c == '_') {
                return false; // part of a longer term, not a keyword
            }
        }
        pos = after;
        return true;
    }

    private boolean matchLiteral(String literal) {
        if (input.regionMatches(pos, literal, 0, literal.length())) {
            pos += literal.length();
            return true;
        }
        return false;
    }

    private boolean match(char c) {
        if (!eof() && peek() == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (!eof() && Character.isWhitespace(peek())) {
            pos++;
        }
    }

    private boolean eof() {
        return pos >= input.length();
    }

    private char peek() {
        return input.charAt(pos);
    }
}

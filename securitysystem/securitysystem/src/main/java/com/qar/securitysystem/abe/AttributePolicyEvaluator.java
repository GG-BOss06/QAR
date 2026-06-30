package com.qar.securitysystem.abe;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AttributePolicyEvaluator {
    public boolean evaluate(String policy, Set<String> attributes) {
        String raw = policy == null ? "" : policy.trim();
        if (raw.isBlank()) {
            return true;
        }
        Parser parser = new Parser(tokenize(raw), normalizeAttributes(attributes));
        boolean result = parser.parseExpression();
        if (parser.hasRemaining()) {
            throw new IllegalArgumentException("invalid_policy_expression");
        }
        return result;
    }

    private static List<Token> tokenize(String policy) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder atom = new StringBuilder();
        for (int i = 0; i < policy.length(); i++) {
            char ch = policy.charAt(i);
            if (Character.isWhitespace(ch)) {
                flushAtom(tokens, atom);
                continue;
            }
            if (ch == '(') {
                flushAtom(tokens, atom);
                tokens.add(new Token(TokenType.LPAREN, "("));
                continue;
            }
            if (ch == ')') {
                flushAtom(tokens, atom);
                tokens.add(new Token(TokenType.RPAREN, ")"));
                continue;
            }
            if (ch == ',' || ch == ';') {
                flushAtom(tokens, atom);
                tokens.add(new Token(TokenType.AND, "AND"));
                continue;
            }
            if (ch == '&' && i + 1 < policy.length() && policy.charAt(i + 1) == '&') {
                flushAtom(tokens, atom);
                tokens.add(new Token(TokenType.AND, "AND"));
                i++;
                continue;
            }
            if (ch == '|' && i + 1 < policy.length() && policy.charAt(i + 1) == '|') {
                flushAtom(tokens, atom);
                tokens.add(new Token(TokenType.OR, "OR"));
                i++;
                continue;
            }
            atom.append(ch);
        }
        flushAtom(tokens, atom);
        return tokens;
    }

    private static void flushAtom(List<Token> tokens, StringBuilder atom) {
        if (atom.length() == 0) {
            return;
        }
        String value = atom.toString().trim();
        atom.setLength(0);
        if (value.isBlank()) {
            return;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        if ("AND".equals(upper)) {
            tokens.add(new Token(TokenType.AND, "AND"));
        } else if ("OR".equals(upper)) {
            tokens.add(new Token(TokenType.OR, "OR"));
        } else {
            tokens.add(new Token(TokenType.ATOM, normalizeToken(value)));
        }
    }

    private static Set<String> normalizeAttributes(Set<String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Set.of();
        }
        return attributes.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(AttributePolicyEvaluator::normalizeToken)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static String normalizeToken(String token) {
        return token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
    }

    private enum TokenType {
        ATOM, AND, OR, LPAREN, RPAREN
    }

    private record Token(TokenType type, String value) {
    }

    private static class Parser {
        private final List<Token> tokens;
        private final Set<String> attributes;
        private int index;

        private Parser(List<Token> tokens, Set<String> attributes) {
            this.tokens = tokens;
            this.attributes = attributes;
        }

        boolean hasRemaining() {
            return index < tokens.size();
        }

        boolean parseExpression() {
            boolean value = parseTerm();
            while (match(TokenType.OR)) {
                boolean right = parseTerm();
                value = value || right;
            }
            return value;
        }

        boolean parseTerm() {
            boolean value = parseFactor();
            while (true) {
                if (match(TokenType.AND)) {
                    boolean right = parseFactor();
                    value = value && right;
                    continue;
                }
                if (peekType(TokenType.ATOM) || peekType(TokenType.LPAREN)) {
                    boolean right = parseFactor();
                    value = value && right;
                    continue;
                }
                break;
            }
            return value;
        }

        boolean parseFactor() {
            if (match(TokenType.LPAREN)) {
                boolean inner = parseExpression();
                expect(TokenType.RPAREN);
                return inner;
            }
            Token token = expect(TokenType.ATOM);
            return attributes.contains(token.value());
        }

        private boolean match(TokenType type) {
            if (peekType(type)) {
                index++;
                return true;
            }
            return false;
        }

        private boolean peekType(TokenType type) {
            return index < tokens.size() && tokens.get(index).type() == type;
        }

        private Token expect(TokenType type) {
            if (index >= tokens.size() || tokens.get(index).type() != type) {
                throw new IllegalArgumentException("invalid_policy_expression");
            }
            return tokens.get(index++);
        }
    }
}

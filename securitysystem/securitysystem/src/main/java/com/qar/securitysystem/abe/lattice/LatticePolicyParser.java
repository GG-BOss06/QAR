package com.qar.securitysystem.abe.lattice;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LatticePolicyParser {
    public LatticePolicyNode parse(String policy) {
        List<Token> tokens = tokenize(policy);
        if (tokens.isEmpty()) {
            return null;
        }
        Parser parser = new Parser(tokens);
        LatticePolicyNode root = parser.parseExpression();
        parser.expect(TokenType.END);
        return root;
    }

    private static List<Token> tokenize(String policy) {
        ArrayList<Token> tokens = new ArrayList<>();
        if (policy == null || policy.isBlank()) {
            return tokens;
        }
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
        tokens.add(new Token(TokenType.END, ""));
        return tokens;
    }

    private static void flushAtom(List<Token> tokens, StringBuilder atom) {
        if (atom.isEmpty()) {
            return;
        }
        String raw = atom.toString().trim();
        atom.setLength(0);
        if (raw.isEmpty()) {
            return;
        }
        String upper = raw.toUpperCase(Locale.ROOT);
        if ("AND".equals(upper)) {
            tokens.add(new Token(TokenType.AND, raw));
        } else if ("OR".equals(upper)) {
            tokens.add(new Token(TokenType.OR, raw));
        } else {
            tokens.add(new Token(TokenType.ATOM, raw.toLowerCase(Locale.ROOT)));
        }
    }

    private enum TokenType {
        AND,
        OR,
        LPAREN,
        RPAREN,
        ATOM,
        END
    }

    private record Token(TokenType type, String value) {
    }

    private static final class Parser {
        private final List<Token> tokens;
        private int index;
        private int nextNodeId = 1;

        private Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        private LatticePolicyNode parseExpression() {
            LatticePolicyNode value = parseTerm();
            while (match(TokenType.OR)) {
                value = collapse(LatticePolicyNode.Type.OR, value, parseTerm());
            }
            return value;
        }

        private LatticePolicyNode parseTerm() {
            LatticePolicyNode value = parseFactor();
            while (true) {
                if (match(TokenType.AND)) {
                    value = collapse(LatticePolicyNode.Type.AND, value, parseFactor());
                    continue;
                }
                if (peek(TokenType.ATOM) || peek(TokenType.LPAREN)) {
                    value = collapse(LatticePolicyNode.Type.AND, value, parseFactor());
                    continue;
                }
                break;
            }
            return value;
        }

        private LatticePolicyNode parseFactor() {
            if (match(TokenType.LPAREN)) {
                LatticePolicyNode inner = parseExpression();
                expect(TokenType.RPAREN);
                return inner;
            }
            Token token = expect(TokenType.ATOM);
            return LatticePolicyNode.leaf(nextNodeId++, token.value());
        }

        private boolean match(TokenType type) {
            if (peek(type)) {
                index++;
                return true;
            }
            return false;
        }

        private boolean peek(TokenType type) {
            return index < tokens.size() && tokens.get(index).type() == type;
        }

        private Token expect(TokenType type) {
            if (!peek(type)) {
                throw new IllegalArgumentException("invalid_policy_expression");
            }
            return tokens.get(index++);
        }

        private LatticePolicyNode collapse(LatticePolicyNode.Type type, LatticePolicyNode left, LatticePolicyNode right) {
            ArrayList<LatticePolicyNode> children = new ArrayList<>();
            appendChildren(type, children, left);
            appendChildren(type, children, right);
            return LatticePolicyNode.branch(nextNodeId++, type, children);
        }

        private static void appendChildren(LatticePolicyNode.Type type, List<LatticePolicyNode> children, LatticePolicyNode node) {
            if (node != null && node.getType() == type) {
                children.addAll(node.getChildren());
                return;
            }
            if (node != null) {
                children.add(node);
            }
        }
    }
}

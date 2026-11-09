package com.gberguy.rei.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public final class ModelExpression {
    private final String source;
    private int cursor;

    private ModelExpression(String source) {
        this.source = source.toLowerCase(java.util.Locale.ROOT);
    }

    public static DoubleUnaryOperator compile(String source) {
        ModelExpression parser = new ModelExpression(source);
        DoubleUnaryOperator result = parser.sum();
        parser.space();
        if (parser.cursor != parser.source.length()) {
            throw parser.invalid();
        }
        return time -> {
            double value = result.applyAsDouble(time);
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Non-finite model expression: " + source);
            }
            return value;
        };
    }

    private DoubleUnaryOperator sum() {
        DoubleUnaryOperator result = product();
        while (true) {
            DoubleUnaryOperator left = result;
            if (take('+')) {
                DoubleUnaryOperator right = product();
                result = time -> left.applyAsDouble(time) + right.applyAsDouble(time);
            } else if (take('-')) {
                DoubleUnaryOperator right = product();
                result = time -> left.applyAsDouble(time) - right.applyAsDouble(time);
            } else {
                return result;
            }
        }
    }

    private DoubleUnaryOperator product() {
        DoubleUnaryOperator result = atom();
        while (true) {
            DoubleUnaryOperator left = result;
            if (take('*')) {
                DoubleUnaryOperator right = atom();
                result = time -> left.applyAsDouble(time) * right.applyAsDouble(time);
            } else if (take('/')) {
                DoubleUnaryOperator right = atom();
                result = time -> left.applyAsDouble(time) / right.applyAsDouble(time);
            } else if (take('%')) {
                DoubleUnaryOperator right = atom();
                result = time -> left.applyAsDouble(time) % right.applyAsDouble(time);
            } else {
                return result;
            }
        }
    }

    private DoubleUnaryOperator atom() {
        if (take('+')) {
            return atom();
        }
        if (take('-')) {
            DoubleUnaryOperator value = atom();
            return time -> -value.applyAsDouble(time);
        }
        if (take('(')) {
            DoubleUnaryOperator value = sum();
            require(')');
            return value;
        }
        space();
        int start = cursor;
        if (cursor < source.length() && (Character.isDigit(source.charAt(cursor)) || source.charAt(cursor) == '.')) {
            while (cursor < source.length() && (Character.isDigit(source.charAt(cursor)) || source.charAt(cursor) == '.')) {
                cursor++;
            }
            if (cursor < source.length() && source.charAt(cursor) == 'e') {
                cursor++;
                if (cursor < source.length() && (source.charAt(cursor) == '+' || source.charAt(cursor) == '-')) cursor++;
                while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) cursor++;
            }
            double value = Double.parseDouble(source.substring(start, cursor));
            return time -> value;
        }
        while (cursor < source.length() && (Character.isLetterOrDigit(source.charAt(cursor))
                || source.charAt(cursor) == '_' || source.charAt(cursor) == '.')) cursor++;
        String name = source.substring(start, cursor);
        if (name.equals("query.anim_time") || name.equals("q.anim_time")) return time -> time;
        if (name.equals("math.pi")) return time -> Math.PI;
        require('(');
        List<DoubleUnaryOperator> arguments = new ArrayList<>();
        if (!take(')')) {
            do { arguments.add(sum()); } while (take(','));
            require(')');
        }
        int count = name.equals("math.clamp") ? 3 : name.equals("math.min") || name.equals("math.max") ? 2 : 1;
        if (arguments.size() != count) throw invalid();
        switch (name) {
            case "math.sin": return time -> Math.sin(Math.toRadians(arguments.get(0).applyAsDouble(time)));
            case "math.cos": return time -> Math.cos(Math.toRadians(arguments.get(0).applyAsDouble(time)));
            case "math.abs": return time -> Math.abs(arguments.get(0).applyAsDouble(time));
            case "math.min": return time -> Math.min(arguments.get(0).applyAsDouble(time), arguments.get(1).applyAsDouble(time));
            case "math.max": return time -> Math.max(arguments.get(0).applyAsDouble(time), arguments.get(1).applyAsDouble(time));
            case "math.clamp": return time -> Math.max(arguments.get(1).applyAsDouble(time), Math.min(arguments.get(2).applyAsDouble(time), arguments.get(0).applyAsDouble(time)));
            default: throw invalid();
        }
    }

    private void require(char token) {
        if (!take(token)) throw invalid();
    }

    private boolean take(char token) {
        space();
        if (cursor < source.length() && source.charAt(cursor) == token) {
            cursor++;
            return true;
        }
        return false;
    }

    private void space() {
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) cursor++;
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unsupported model expression at " + cursor + ": " + source);
    }
}

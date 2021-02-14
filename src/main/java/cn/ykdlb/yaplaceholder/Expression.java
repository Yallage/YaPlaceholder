package cn.ykdlb.yaplaceholder;

import cn.ykdlb.yaplaceholder.exception.UnknownDataTypeException;
import cn.ykdlb.yaplaceholder.exception.UnknownOperatorException;
import cn.ykdlb.yaplaceholder.function.Function;
import cn.ykdlb.yaplaceholder.function.special.ValueFunction;
import cn.ykdlb.yaplaceholder.operator.Operator;
import cn.ykdlb.yaplaceholder.operator.arithmetic.Minus;
import cn.ykdlb.yaplaceholder.operator.logical.LogicalNot;
import cn.ykdlb.yaplaceholder.operator.special.Bracket;
import cn.ykdlb.yaplaceholder.operator.special.StartParam;
import cn.ykdlb.yaplaceholder.type.ObjectType;
import cn.ykdlb.yaplaceholder.type.Type;
import cn.ykdlb.yaplaceholder.value.BooleanValue;
import cn.ykdlb.yaplaceholder.value.IntegerValue;
import cn.ykdlb.yaplaceholder.value.StringValue;
import cn.ykdlb.yaplaceholder.value.Value;
import me.clip.placeholderapi.PlaceholderAPI;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.logging.Logger;

public class Expression extends ArrayList<Component> {
    public static final String ERROR_STRING = "§4Error";
    public static final Expression ERROR = new Expression("\"" + ERROR_STRING + "\"") {{
        add(new StringValue(ERROR_STRING, 0));
    }};
    private static final Logger logger = YaPlaceholder.getInstance().getLogger();
    private static final Set<Character> OPERATOR_CHARSET = new HashSet<>() {{
        addAll(Arrays.asList('+', '-', '*', '/', '<', '>', '=', '!', '&', '|', '^', '#', '(', ')'));
    }};

    private final String infix;

    public Expression(String infix) {
        super();
        this.infix = infix;
    }

    public static Expression fromInfixExpression(String infix) {
        Expression expression = new Expression(infix);
        Stack<Operator> operators = new Stack<>();
        StringBuilder builder = new StringBuilder();
        Status status = Status.NONE;
        boolean isInString = false;
        Stack<Function> depth = new Stack<>();
        Stack<MutableInt> paramCounter = new Stack<>();
        for (int i = 0; i < infix.length(); i++) {
            char ch = infix.charAt(i);
            if (!isInString) {
                if (ch == '(') {
                    if (status == Status.EXPRESSION) {
                        paramCounter.push(new MutableInt(0));
                        String functionString = builder.toString();
                        if (Function.has(functionString)) {
                            Function function = Function.get(functionString);
                            expression.add(new StartParam(i));
                            depth.push(function);
                            status = Status.OPERATOR;
                        } else {
                            printErrorMessage(infix, "Unknown function name: " + functionString + " .", i);
                            return ERROR;
                        }
                        builder.delete(0, builder.length());
                    } else {
                        depth.push(null);
                        if (builder.length() > 0) try {
                            Operator operator = Operator.getOperator(builder.toString(), i);
                            while (!operators.empty() && operator.getPriority() <= operators.peek().getPriority())
                                expression.add(operators.pop());
                            operators.add(operator);
                            builder.delete(0, builder.length());
                        } catch (UnknownOperatorException e) {
                            printErrorMessage(infix, e.getMessage(), i);
                            return ERROR;
                        }
                    }
                    operators.push(new Bracket(i));
                } else if (ch == ')') {
                    if (depth.empty()) {
                        printErrorMessage(infix, "Unexpected ')'.", i);
                        return ERROR;
                    }
                    if (builder.length() > 0) try {
                        expression.add(Value.getValueFromString(builder.toString(), i - builder.length()));
                        builder.delete(0, builder.length());
                    } catch (UnknownDataTypeException e) {
                        printErrorMessage(infix, e.getMessage(), i - builder.length());
                        return ERROR;
                    }
                    while (!operators.empty() && !(operators.peek() instanceof Bracket))
                        expression.add(operators.pop());
                    if (operators.empty()) {
                        printErrorMessage(infix, "Unexpected ')'.", i);
                        return ERROR;
                    } else operators.pop();
                    if (depth.peek() != null) {
                        paramCounter.peek().add(1);
                        Function function = depth.peek();
                        int count = paramCounter.peek().toInteger();
                        if (function.isParamsArray()) {
                            if (function.getParamsType().size() > count) {
                                printErrorMessage(infix, String.format("Expected at least %d arguments, found %d.", function.getParamsType().size(), count), i);
                                return ERROR;
                            }
                        } else {
                            if (function.getParamsType().size() != count) {
                                printErrorMessage(infix, String.format("Expected %d arguments, found %d.", function.getParamsType().size(), count), i);
                                return ERROR;
                            }
                        }
                        expression.add(function);
                        paramCounter.pop();
                    }
                    status = Status.OPERATOR;
                    depth.pop();
                    if (!operators.empty() && operators.peek() instanceof LogicalNot) expression.add(operators.pop());
                } else if (ch == ',') {
                    if (depth.peek() != null) {
                        paramCounter.peek().add(1);
                        if (builder.length() > 0) try {
                            expression.add(Value.getValueFromString(builder.toString(), i - builder.length()));
                            builder.delete(0, builder.length());
                        } catch (UnknownDataTypeException e) {
                            printErrorMessage(infix, e.getMessage(), i - builder.length());
                            return ERROR;
                        }
                        status = Status.EXPRESSION;
                        while (!operators.empty() && !(operators.peek() instanceof Bracket))
                            expression.add(operators.pop());
                    } else {
                        printErrorMessage(infix, "Unexpected ','.", i);
                        return ERROR;
                    }
                } else if (OPERATOR_CHARSET.contains(ch)) {
                    if (status == Status.OPERATOR || status == Status.NONE) {
                        if (builder.length() == 0) expression.add(new IntegerValue(0, i));
                        builder.append(ch);
                        status = Status.OPERATOR;
                    } else {
                        if ((builder.length() != 0 || ch != '!' || infix.charAt(i + 1) == '=' ) && (builder.length() != 0 || ch != '-')) try {
                            expression.add(Value.getValueFromString(builder.toString(), i - builder.length()));
                            status = Status.OPERATOR;
                            builder.delete(0, builder.length());
                            builder.append(ch);
                            if (!operators.empty() && operators.peek() instanceof LogicalNot)
                                expression.add(operators.pop());
                        } catch (UnknownDataTypeException e) {
                            printErrorMessage(infix, e.getMessage(), i - builder.length());
                            return ERROR;
                        }
                        else {
                            if (ch == '!') operators.push(new LogicalNot(i));
                            if (ch == '-') {
                                expression.add(new IntegerValue(0, i));
                                operators.push(new Minus(i));
                            }
                        }
                    }
                } else {
                    if (status == Status.EXPRESSION || status == Status.NONE) {
                        builder.append(ch);
                        if (status == Status.NONE && ch == '"') isInString = true;
                        status = Status.EXPRESSION;
                    } else {
                        if (builder.length() > 0) try {
                            Operator operator = Operator.getOperator(builder.toString(), i);
                            while (!operators.empty() && operator.getPriority() <= operators.peek().getPriority())
                                expression.add(operators.pop());
                            operators.add(operator);
                            builder.delete(0, builder.length());
                        } catch (UnknownOperatorException e) {
                            printErrorMessage(infix, e.getMessage(), i);
                            return ERROR;
                        }
                        status = Status.EXPRESSION;
                        builder.append(ch);
                        if (ch == '"') isInString = true;
                    }
                }
            } else {
                builder.append(ch);
                if (ch == '"') {
                    isInString = false;
                }
            }
        }
        if (!depth.empty()) {
            printErrorMessage(infix, "Expected ')'.", infix.length());
            return ERROR;
        }
        if (builder.length() > 0) try {
            expression.add(Value.getValueFromString(builder.toString(), infix.length() - builder.length()));
        } catch (UnknownDataTypeException e) {
            printErrorMessage(infix, e.getMessage(), infix.length() - builder.length());
            return ERROR;
        }
        while (!operators.empty()) expression.add(operators.pop());
        return expression;
    }

    private static void printErrorMessage(String expression, String message, int column) {
        logger.warning(ChatColor.RED + "Error at column " + column + ": " + message);
        logger.warning(ChatColor.RED + expression);
        logger.warning(ChatColor.RED + " ".repeat(column) + "^");
    }

    public String calculateValue(OfflinePlayer player) {
        Stack<Component> valueStack = new Stack<>();
        for (Component component : this) {
            if (component instanceof Value) valueStack.push(component);
            if (component instanceof Operator) {
                if (component instanceof LogicalNot) {
                    if (!valueStack.empty() && valueStack.peek() instanceof BooleanValue) {
                        valueStack.push(new BooleanValue(!((BooleanValue) valueStack.pop()).getValue(), component.getColumn()));
                    } else {
                        printErrorMessage(infix, "Unexpected '!'.", component.getColumn());
                        return ERROR_STRING;
                    }
                } else if (component instanceof StartParam) {
                    valueStack.push(component);
                } else {
                    Value<?> value2 = (Value<?>) valueStack.pop();
                    Value<?> value1 = (Value<?>) valueStack.pop();
                    try {
                        valueStack.push(value1.operation((Operator) component, value2));
                    } catch (Exception e) {
                        printErrorMessage(infix, e.getMessage(), component.getColumn());
                        return ERROR_STRING;
                    }
                }
            }
            if (component instanceof Function) {
                List<Value<?>> paramList = new ArrayList<>();
                while (!(valueStack.peek() instanceof StartParam)) paramList.add((Value<?>) valueStack.pop());
                valueStack.pop();
                Collections.reverse(paramList);
                List<Type<?>> paramsTypes = ((Function) component).getParamsType();
                for (int i = 0; i < paramList.size(); i++) {
                    Value<?> value = paramList.get(i);
                    Type<?> type = paramsTypes.get(Math.min(i, paramsTypes.size() - 1));
                    if (type instanceof ObjectType) continue;
                    if (value.getType() != type) paramList.set(i, type.cast(value));
                }
                if (component instanceof ValueFunction) {
                    valueStack.push(new StringValue(PlaceholderAPI.setPlaceholders(player, "%" + paramList.get(0).getString() + "%"), component.getColumn()));
                } else {
                    valueStack.push(((Function) component).invoke(paramList.toArray(new Value<?>[0])));
                }
            }
        }
        return ((Value<?>) valueStack.pop()).getString();
    }

    private enum Status {
        NONE, EXPRESSION, OPERATOR
    }
}
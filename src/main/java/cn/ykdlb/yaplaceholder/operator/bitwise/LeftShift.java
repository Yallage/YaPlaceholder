package cn.ykdlb.yaplaceholder.operator.bitwise;

import cn.ykdlb.yaplaceholder.operator.Operator;
import org.jetbrains.annotations.NotNull;

public class LeftShift extends Operator {
    public LeftShift(int column) {
        super(column);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public @NotNull String getString() {
        return "<<";
    }
}

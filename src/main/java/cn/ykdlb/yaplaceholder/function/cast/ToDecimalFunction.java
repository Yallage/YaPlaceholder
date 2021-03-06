package cn.ykdlb.yaplaceholder.function.cast;

import cn.ykdlb.yaplaceholder.function.Function;
import cn.ykdlb.yaplaceholder.type.Type;
import cn.ykdlb.yaplaceholder.value.DecimalValue;
import cn.ykdlb.yaplaceholder.value.IntegerValue;
import cn.ykdlb.yaplaceholder.value.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ToDecimalFunction extends Function {

    private final List<List<Type<?>>> paramsType = Collections.singletonList(
            Collections.singletonList(Type.get("Object"))
    );

    public ToDecimalFunction(int column) {
        super(column);
    }

    @Override
    public String getName() {
        return "toDecimal";
    }

    @Override
    public List<List<Type<?>>> getParamsType() {
        return paramsType;
    }

    @Override
    public boolean isLastVarParam() {
        return false;
    }

    @Override
    public DecimalValue invoke(Value<?>[] params) {
        return ((DecimalValue) Type.get("Decimal").cast(params[0]));
    }
}

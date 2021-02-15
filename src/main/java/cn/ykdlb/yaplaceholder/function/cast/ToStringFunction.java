package cn.ykdlb.yaplaceholder.function.cast;

import cn.ykdlb.yaplaceholder.function.Function;
import cn.ykdlb.yaplaceholder.type.Type;
import cn.ykdlb.yaplaceholder.value.IntegerValue;
import cn.ykdlb.yaplaceholder.value.StringValue;
import cn.ykdlb.yaplaceholder.value.Value;

import java.util.Collections;
import java.util.List;

public class ToStringFunction extends Function {

    private final List<List<Type<?>>> paramsType = Collections.singletonList(
            Collections.singletonList(Type.get("Object"))
    );

    public ToStringFunction(int column) {
        super(column);
    }

    @Override
    public String getName() {
        return "toString";
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
    public StringValue invoke(Value<?>[] params) {
        return ((StringValue) Type.get("String").cast(params[0]));
    }
}

package pt.ist.fenixframework.dml;

import java.util.List;

public class ArrayValueType implements ValueType {

    private ValueType baseType;
    private String arraySpec;

    public ArrayValueType(ValueType baseType, String arraySpec) {
        this.baseType = baseType;
        this.arraySpec = arraySpec;
    }

    public PlainValueType getBaseType() {
        return baseType.getBaseType();
    }

    public String getDomainName() {
        return baseType.getDomainName();
    }

    public String getFullname() {
        return baseType.getFullname();
    }

    public boolean isBuiltin() {
        return baseType.isBuiltin();
    }

    public boolean isEnum() {
        return baseType.isEnum();
    }

    public List<ExternalizationElement> getExternalizationElements() {
        return baseType.getExternalizationElements();
    }

    public String getInternalizationMethodName() {
        return baseType.getInternalizationMethodName();
    }
}

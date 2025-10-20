package tech.kaffa.portrait.tests.fixtures;

import java.util.List;
import java.util.Map;
import tech.kaffa.portrait.Reflective;

@Reflective
public class ParameterTestClass {

    public String primitiveParams(byte byteVal,
                                  short shortVal,
                                  int intVal,
                                  long longVal,
                                  float floatVal,
                                  double doubleVal,
                                  boolean boolVal,
                                  char charVal) {
        return "primitives";
    }

    public String objectParams(String stringVal, List<String> listVal, Map<String, Integer> mapVal) {
        return "objects";
    }

    public String arrayParams(int[] intArray, String[] stringArray) {
        return "arrays";
    }

    public String varargParams(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values[i]);
        }
        return builder.toString();
    }

    public <T> T genericMethod(T value) {
        return value;
    }
}

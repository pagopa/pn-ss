package it.pagopa.pnss.common.utils;

import java.util.Arrays;
import java.util.List;

public class CompareUtils {

    private CompareUtils(){}

    public static <T> boolean listContainsAny(List<T> list, List<T> elements) {
        return list.stream().anyMatch(elements::contains);
    }

    public static <T extends Enum<T>> boolean enumContainsAny(Class<T> enumeration,List<T> elements){
        return listContainsAny(Arrays.asList(enumeration.getEnumConstants()),elements);
    }
}

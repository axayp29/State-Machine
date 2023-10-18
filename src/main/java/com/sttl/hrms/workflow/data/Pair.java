package com.sttl.hrms.workflow.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@AllArgsConstructor
@NoArgsConstructor // required by Kryo
@Data
public class Pair<T, U> {

    private T first;
    private U second;

    public static <K, V> Map<K, V> pairListToMap(List<Pair<K, V>> pairList) {
        return pairList
                .stream()
                .collect(toMap(Pair::getFirst, Pair::getSecond));
    }


}

package com.rajanainart.common.data;

import java.util.List;

public interface BaseMessageTable {
    String getId    ();
    String getName  ();
    String getTarget();

    List<BaseMessageColumn> getColumns();
}

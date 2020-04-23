package com.rajanainart.data;

import java.util.List;

public interface BaseMessageTable {
    String getId    ();
    String getName  ();
    String getTarget();

    List<BaseMessageColumn> getColumns();
}

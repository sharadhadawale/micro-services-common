package com.rajanainart.rest.hierarchy;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.BaseMessageColumn;
import com.rajanainart.data.DbCol;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("BaseHierarchy")
public class BaseHierarchy extends BaseEntity {
    private double hierarchyId;
    @DbCol(name = "HIERARCHY_ID", type = BaseMessageColumn.ColumnType.NUMERIC)
    @JsonProperty("HIERARCHY_ID")
    public long getHierarchyId() { return (long)hierarchyId; }

    @DbCol(name = "HIERARCHY_ID", type = BaseMessageColumn.ColumnType.NUMERIC)
    public void setHierarchyId(double value) { hierarchyId = value; }

    private String name;
    @DbCol(name = "NAME", type = BaseMessageColumn.ColumnType.TEXT)
    @JsonProperty("NAME")
    public String getName() { return name; }

    @DbCol(name = "NAME", type = BaseMessageColumn.ColumnType.TEXT)
    public void setName(String value) { name = value; }

    private String description;
    @DbCol(name = "DESCRIPTION", type = BaseMessageColumn.ColumnType.TEXT)
    @JsonProperty("DESCRIPTION")
    public String getDescription() { return description; }

    @DbCol(name = "DESCRIPTION", type = BaseMessageColumn.ColumnType.TEXT)
    public void setDescription(String value) { description = value; }

    private double levelType;
    @DbCol(name = "HIERARCHY_LEVEL_TYPE", type = BaseMessageColumn.ColumnType.NUMERIC)
    @JsonProperty("HIERARCHY_LEVEL_TYPE")
    public int getHierarchyLevel() { return (int)levelType; }

    @DbCol(name = "HIERARCHY_LEVEL_TYPE", type = BaseMessageColumn.ColumnType.NUMERIC)
    public void setHierarchyLevel(double value) { levelType = value; }

    private double hierarchyParentId;
    @DbCol(name = "HIERARCHY_ID_PARENT", type = BaseMessageColumn.ColumnType.NUMERIC)
    @JsonProperty("HIERARCHY_ID_PARENT")
    public long getHierarchyParentId() { return (long)hierarchyParentId; }

    @DbCol(name = "HIERARCHY_ID_PARENT", type = BaseMessageColumn.ColumnType.NUMERIC)
    public void setHierarchyParentId(double value) { hierarchyParentId = value; }

    private List<BaseHierarchy> children = new ArrayList<>();
    @JsonProperty("CHILDREN")
    public List<BaseHierarchy> getChildren() { return children; }
}

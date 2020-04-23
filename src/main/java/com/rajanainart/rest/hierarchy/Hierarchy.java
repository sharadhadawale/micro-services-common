package com.rajanainart.rest.hierarchy;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.Database;
import com.rajanainart.data.QueryExecutor;
import com.rajanainart.rest.RestQueryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Hierarchy {
    private String   query = "";
    private Database db;
    private TableDetails     dbDetails    ;
    private RestQueryRequest queryRequest ;
    private QueryExecutor    queryExecutor;

    public Hierarchy(Database db, String query) {
        this.query = query;
        this.db    = db   ;
    }

    public Hierarchy(Database db, TableDetails dbDetails, RestQueryRequest queryRequest) {
        this.dbDetails = dbDetails;
        this.db        = db       ;
        this.queryRequest = queryRequest;
    }

    public Hierarchy(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public String getQuery() { return query; }

    public TableDetails getDbSourceDetails() { return dbDetails; }

    public String getFinalQuery() {
        StringBuilder builder = new StringBuilder();
        if (!query.isEmpty())
            return query;
        else {
            builder.append(String.format("SELECT %s AS hierarchy_id, %s AS name, %s AS description, %s AS hierarchy_level_type,\r\n",
                    dbDetails.getPkColName(), dbDetails.getNameColName(), dbDetails.getDescColName(), dbDetails.getLevelColName()))
                    .append(String.format("%s AS hierarchy_id_parent\r\n", dbDetails.getParentColName()))
                    .append(String.format("FROM %s\r\n", dbDetails.getTableName()))
                    .append(String.format("WHERE %s", queryRequest.buildConditionFromParams()));

            return builder.toString();
        }
    }

    public <T extends BaseHierarchy> List<T> buildHierarchy(Class<T> clazz) {
        List<T> input  = getInput(clazz);
        List<T> result = new ArrayList<>();

        Supplier<Stream<T>> streamSupplier = () -> input.stream();
        Stream<T> roots = streamSupplier.get().filter(x -> x.getHierarchyLevel() == 0 || x.getHierarchyParentId() == 0);

        roots.forEach(x -> {
            result.add(x);
            buildTChildren(streamSupplier, x);
        });
        roots.close();
        return result;
    }

    public List<BaseHierarchy> buildHierarchy() {
        List<BaseHierarchy> input  = getInput();
        List<BaseHierarchy> result = new ArrayList<>();

        Supplier<Stream<BaseHierarchy>> streamSupplier = () -> input.stream();
        Stream<BaseHierarchy> roots = streamSupplier.get().filter(x -> x.getHierarchyLevel() == 0 || x.getHierarchyParentId() == 0);

        roots.forEach(x -> {
            result.add(x);
            buildChildren(streamSupplier, x);
        });
        roots.close();
        return result;
    }

    public <T extends BaseHierarchy> List<BaseEntity> buildHierarchyAsBaseEntity(Class<T> clazz) {
        List<T> input = buildHierarchy(clazz);
        List<BaseEntity> result = new ArrayList<>();

        for (BaseHierarchy h : input) result.add(h);
        return result;
    }

    public List<BaseEntity> buildHierarchyAsBaseEntity() {
        List<BaseHierarchy> input  = buildHierarchy();
        List<BaseEntity   > result = new ArrayList<>();

        for (BaseHierarchy h : input) result.add(h);
        return result;
    }

    private <T extends BaseHierarchy> List<T> getInput(Class<T> clazz) {
        if (queryExecutor == null)
            return db.bindClassList(clazz, getFinalQuery());
        else
            return queryExecutor.fetchClassResultSetAsEntity(clazz);
    }

    private List<BaseHierarchy> getInput() {
        if (queryExecutor == null)
            return db.bindClassList(BaseHierarchy.class, getFinalQuery());
        else
            return queryExecutor.fetchClassResultSetAsEntity(BaseHierarchy.class);
    }

    private <T extends BaseHierarchy> void buildTChildren(Supplier<Stream<T>> hierarchy, BaseHierarchy current) {
        Stream<T> children = hierarchy.get().filter(x -> x.getHierarchyParentId() == current.getHierarchyId());
        children.forEach(x -> {
            current.getChildren().add(x);
            buildTChildren(hierarchy, x);
        });
        children.close();
    }

    private void buildChildren(Supplier<Stream<BaseHierarchy>> hierarchy, BaseHierarchy current) {
        Stream<BaseHierarchy> children = hierarchy.get().filter(x -> x.getHierarchyParentId() == current.getHierarchyId());
        children.forEach(x -> {
            current.getChildren().add(x);
            buildChildren(hierarchy, x);
        });
        children.close();
    }
}

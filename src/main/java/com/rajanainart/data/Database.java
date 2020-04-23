package com.rajanainart.data;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.Date;

import com.rajanainart.data.provider.DbSpecificProvider;
import com.rajanainart.helper.ReflectionHelper;
import com.rajanainart.property.PropertyUtil;
import com.rajanainart.config.AppContext;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.rest.RestQueryConfig;
import org.hibernate.*;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
public final class Database implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Database.class);

    public final static String QUERY_PARAMETER_REGEX = "[\\?\\:]";
    public final static String JDBC_DRIVER_PROPERTY  = "hibernate.dialect";

    public final static Map<String, DbSpecificProvider> PROVIDERS = AppContext.getBeansOfType(DbSpecificProvider.class);

    private static SessionFactory       sessionFactory;
    private static EntityManagerFactory managerFactory;

    private static Map<String, SessionFactory> otherSessionFactories;

    private String             dbKey = "";
    private Session            session     = null;
    private Transaction        transaction = null;
    private DbSpecificProvider provider    = null;

    static {
        if (sessionFactory == null) {
            managerFactory = (EntityManagerFactory)AppContext.getApplicationContext().getBean("entityManagerFactory");
            sessionFactory = managerFactory.unwrap(SessionFactory.class);
        }
        otherSessionFactories = new HashMap<>();
    }

    public static SessionFactory getSessionFactory() { return sessionFactory; }

    public DbSpecificProvider getDbSpecificProvider() { return provider; }

    public Database() {
        init(sessionFactory);
    }

    public Database(int timeoutSeconds, boolean rollbackOnly) {
        this();
        if (transaction != null) {
            if (rollbackOnly)
                transaction.setRollbackOnly();
            transaction.setTimeout(timeoutSeconds);
        }
    }

    public Database(String dbKey) {
        if (dbKey == null || dbKey.isEmpty()) {
            init(sessionFactory);
            return;
        }
        this.dbKey = dbKey;
        SessionFactory factory = null;
        if (otherSessionFactories.containsKey(dbKey))
            factory = otherSessionFactories.get(dbKey);
        else {
            Map<String, String> allProperties = PropertyUtil.getAllProperties();

            Properties properties = new Properties();
            for (Map.Entry<String, String> p : allProperties.entrySet()) {
                String p1 = String.format("%s.property.", dbKey);
                if (!p.getKey().startsWith(p1)) continue;

                String key = p.getKey().substring(p1.length());
                properties.put(key, p.getValue());
            }

            LocalSessionFactoryBuilder sessionBuilder = new LocalSessionFactoryBuilder(buildDataSource(dbKey));
            sessionBuilder.addProperties(properties);
            factory = sessionBuilder.buildSessionFactory();
            otherSessionFactories.put(dbKey, factory);
        }
        init(factory);
    }

    public Database(String dbKey, int timeoutSeconds, boolean rollbackOnly) {
        this(dbKey);
        if (transaction != null) {
            if (rollbackOnly)
                transaction.setRollbackOnly();
            transaction.setTimeout(timeoutSeconds);
        }
    }

    public Transaction getTransaction() { return transaction; }
    public Session 	   getSession	 () { return session    ; }

    public <T> T       find   (Class<T> clazz, int id) { return (T)session.get(clazz, id); }
    public <T> List<T> findAll(Class<T> clazz) { return session.createQuery("FROM " + clazz.getName()).list(); }
    public <T> T       update(T entity) { return (T)session.merge(entity); }
    public <T> void    delete(T entity) { session.delete(entity)         ; }
    public <T> void    save  (T entity) { session.saveOrUpdate(entity)   ; }
    public <T> Integer    saveAndGetId( T entity) { return (Integer) session.save(entity) ;
    }

    public <T> void deleteById(Class<T> clazz, int entityId) {
        T entity = find(clazz, entityId);
        delete(entity);
    }

    public <T, V extends Number> T find(Class<T> clazz, V id) {
        return (T)session.get(clazz, id);
    }

    public <T> T find(Class<T> clazz, String query, Parameter ... parameters) {
        try {
            String noInPrs = buildQueryWithoutArrayParams(query, parameters);
            String updated = provider.getParameterizedQuery(noInPrs);
            Query  q       = session.createNativeQuery(updated, clazz);
            bindNamedParameters(q, noInPrs, parameters);
            return (T)q.getSingleResult();
        }
        catch (SQLException ex) {
            return null;
        }
    }

    public <T> List<T> findMultiple(Class<T> clazz, String query, Parameter ... parameters) {
        try {
            String noInPrs = buildQueryWithoutArrayParams(query, parameters);
            String updated = provider.getParameterizedQuery(noInPrs);
            Query  q       = session.createNativeQuery(updated, clazz);
            bindNamedParameters(q, noInPrs, parameters);
            return q.getResultList();
        }
        catch (SQLException ex) {
            return new ArrayList<>();
        }
    }

    public String getPropertyValue(String property) {
        SessionFactory factory = dbKey.isEmpty() ? sessionFactory : otherSessionFactories.get(dbKey);
        return factory.getProperties().getOrDefault(property, "").toString();
    }

    public String getJdbcDriverPropertyValue() {
        return getPropertyValue(JDBC_DRIVER_PROPERTY);
    }

    private void init(SessionFactory factory) {
        String driverName = getJdbcDriverPropertyValue();
        if (!PROVIDERS.containsKey(driverName))
            throw new NullPointerException(String.format("%s does not have DbSpecificProvider implementation", driverName));

        provider    = PROVIDERS.get(driverName);
        session     = factory.openSession();
        transaction = session.beginTransaction();
    }

    public String getDbKey() { return dbKey; }

    public int executeQueryWithJdbc(String query, Parameter ... parameters) {
        List<Integer> result = new ArrayList<>();
        session.doWork(
                (Connection connection) -> {
                    String noInPrs = buildQueryWithoutArrayParams(query, parameters);
                    String updated = provider.getParameterizedQuery(noInPrs);
                    PreparedStatement statement = connection.prepareStatement(updated);
                    bindNamedParameters(statement, noInPrs, parameters);
                    int r = statement.executeUpdate();
                    statement.close();
                    result.add(r);
                }
        );
        return result.size() > 0 ? result.get(0) : 0;
    }

    private static Connection adhocConnection = null;
    public static Connection getAdhocJdbcConnection() throws SQLException {
        if (adhocConnection == null) {
            adhocConnection = DriverManager.getConnection(PropertyUtil.getPropertyValue(PropertyUtil.PropertyType.JDBC_URL),
                    PropertyUtil.getPropertyValue(PropertyUtil.PropertyType.JDBC_USERNAME),
                    PropertyUtil.getPropertyValue(PropertyUtil.PropertyType.JDBC_PASSWORD));
            adhocConnection.setAutoCommit(false);
        }
        return adhocConnection;
    }

    public static void closeAdhocConnection() {
        try {
            if (adhocConnection != null && !adhocConnection.isClosed()) {
                adhocConnection.close();
                adhocConnection = null;
            }
        }
        catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public List<Map<String, Object>> selectAsMapList(String query, Parameter ... parameters) {
        try {
            String noInPrs = buildQueryWithoutArrayParams(query, parameters);
            String updated = provider.getParameterizedQuery(noInPrs);
            Query  q       = session.createNativeQuery(updated);
            bindNamedParameters(q, noInPrs, parameters);
            q.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
            return q.getResultList();
        }
        catch(SQLException ex) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> selectDbSpecificMapList(String query, RestQueryConfig.RestQueryType queryType, Parameter ... parameters) {
        List<Map<String, Object>> result = null;
        if (queryType == RestQueryConfig.RestQueryType.PROC)
            result = provider.selectDbSpecificMapList(this, query, parameters);
        if (result == null || queryType != RestQueryConfig.RestQueryType.PROC)
            result = selectAsMapList(query, parameters);
        return result;
    }

    public long selectCurrentSequenceValue(String sequenceName) {
        String query = provider.selectCurrentSequenceString(sequenceName);
        return getSequenceValue(query);
    }

    public long selectNextSequenceValue(String sequenceName) {
        String query = String.format("SELECT %s_q.NEXTVAL FROM dual", sequenceName);
        return getSequenceValue(query);
    }

    private long getSequenceValue(String query) {
        List<Long> result = new ArrayList<>();
        selectWithCallback(query, (ResultSet rs, long index) -> {
            try {
                result.add(rs.getLong(1));
            }
            catch(SQLException ex) {
                log.error(String.format("Exception occurred while getting the sequence value:%s", ex.getMessage()));
            }
        });
        return result.size() > 0 ? result.get(0) : 0;
    }

    public Map<String, String> getResultSetColumns(String query, Parameter ... parameters) {
        Map<String, String> columns = new HashMap<>();
        session.doWork(
                (Connection connection) -> {
                    String emptyQuery = String.format("SELECT * FROM (%s) src WHERE 1 != 1", query);
                    String updated    = provider.getParameterizedQuery(emptyQuery);
                    PreparedStatement statement = connection.prepareStatement(updated);
                    bindNamedParameters(statement, emptyQuery, parameters);
                    ResultSet rs = statement.executeQuery();
                    for (int idx=1; idx<=rs.getMetaData().getColumnCount(); idx++)
                        columns.put(rs.getMetaData().getColumnName(idx), rs.getMetaData().getColumnTypeName(idx));
                    rs.close();
                    statement.close();
                }
        );
        return columns;
    }

    public void selectWithCallback(String query, DataResultSet callback, Parameter ... parameters) {
        session.doWork(
                (Connection connection) -> {
                    String noInPrs = buildQueryWithoutArrayParams(query, parameters);
                    String updated = provider.getParameterizedQuery(noInPrs);
                    PreparedStatement statement = connection.prepareStatement(updated);
                    bindNamedParameters(statement, noInPrs, parameters);
                    ResultSet rs = statement.executeQuery();
                    long index = 0;
                    while (rs.next())
                        callback.process(rs, index++);
                    rs.close();
                    statement.close();
                }
        );
    }

    public void selectWithCallback(String query, DataRecord callback, Parameter ... parameters) {
        try {
            String noInPrs = buildQueryWithoutArrayParams(query, parameters);
            String updated = provider.getParameterizedQuery(noInPrs);
            Query  q       = session.createNativeQuery(updated);
            bindNamedParameters(q, noInPrs, parameters);
            q.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
            List<Map<String, Object>> rows = q.getResultList();
            for (Map<String, Object> row : rows)
                callback.process(row);
        }
        catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public <T extends Number> T selectScalar(String query, Parameter ... parameters) {
        List<T> instance = new ArrayList<>();
        selectWithCallback(query, (record, index) -> {
            try {
                Object result = record.getObject(1);
                instance.add((T)result);
            }
            catch(Exception ex) {
                log.error("Error while selectScalar");
                ex.printStackTrace();
            }
        }, parameters);
        return instance.size() > 0 ? instance.get(0) : null;
    }

    public <T extends BaseEntity> List<T> bindList(Class<T> clazz, String query, Parameter ... parameters) {
        List<T> list = new ArrayList<>();
        selectWithCallback(query, (ResultSet row, long currentRowIndex) -> {
            T instance = bind(clazz, row);
            if (instance != null)
                list.add(instance);
        }, parameters);
        return list;
    }

    public <T extends BaseEntity> List<T> bindList(Class<T> clazz, String query, ValidateRecord validate, Parameter ... parameters) {
        List<T> list = new ArrayList<>();
        selectWithCallback(query, (ResultSet row, long currentRowIndex) -> {
            if (validate.process(row, currentRowIndex)) {
                T instance = bind(clazz, row);
                if (instance != null)
                    list.add(instance);
            }
        }, parameters);
        return list;
    }

    public <T> List<T> bindClassList(Class<T> clazz, String query, Parameter ... parameters) {
        List<T> list = new ArrayList<>();
        selectWithCallback(query, (ResultSet row, long currentRowIndex) -> {
            T instance = bindClass(clazz, row);
            if (instance != null)
                list.add(instance);
        }, parameters);
        return list;
    }

    public <T extends BaseEntity> T bind(Class<T> clazz, String query, Parameter ... parameters) {
        List<T> list = new ArrayList<>();
        selectWithCallback(query, (ResultSet row, long currentRowIndex) -> {
            if (currentRowIndex == 0) {
                T instance = bind(clazz, row);
                list.add(instance);
            }
        }, parameters);
        return list.size() > 0 ? list.get(0) : null;
    }

    public static <T> T bindClass(Class<T> clazz, ResultSet row) {
        try {
            T instance = clazz.newInstance();
            Map<String, Method> methods = ReflectionHelper.getAnnotatedSetMethods(clazz, DbCol.class);
            for (Map.Entry<String, Method> column : methods.entrySet())
                bindColumn(instance, column, row);

            return instance;
        }
        catch(Exception ex) {
            log.error(String.format("Exception occurred while binding recordset with entity:%s", ex.getMessage()));
        }
        return null;
    }

    public String buildQueryWithoutArrayParams(String query, Parameter ... parameters) {
        String result = query;
        for (Parameter p : parameters) {
            if (p.getValue().getClass().getName().equalsIgnoreCase(LIST_CLASS_NAME)) {
                ArrayList params = (ArrayList)p.getValue();
                if (params.size() > 0)
                    result = result.replaceAll(String.format("%s%s", Database.QUERY_PARAMETER_REGEX, p.getName()),
                                               MiscHelper.buildArrayToString(params));
                else
                    result = result.replaceAll(String.format("%s%s", Database.QUERY_PARAMETER_REGEX, p.getName()), "''");
            }
        }
        return result;
    }

    private static <T> void bindColumn(T instance, Map.Entry<String, Method> column, ResultSet row) {
        try {
            Method method = column.getValue();
            Annotation a  = ReflectionHelper.getAnnotation(method, DbCol.class);
            BaseMessageColumn.ColumnType type = BaseMessageColumn.ColumnType.valueOf(String.valueOf(AnnotationUtils.getValue(a, "type")));

            switch (type) {
                case INTEGER:
                    method.invoke(instance, row.getLong(column.getKey()));
                    break;
                case NUMERIC:
                    method.invoke(instance, row.getDouble(column.getKey()));
                    break;
                case DATE:
                    method.invoke(instance, row.getDate(column.getKey()));
                    break;
                default:
                    method.invoke(instance, row.getString(column.getKey()));
                    break;
            }
        }
        catch(Exception e) {
            if (!column.getKey().equalsIgnoreCase("ID"))
                log.error(String.format("Exception occurred while getting field '%s' value from recordset:%s", column, e.getMessage()));
        }
    }

    private static <T> void bindColumn1(T instance, Map.Entry<String, Method> column, ResultSet row) {
        try {
            Method method = column.getValue();
            DbCol dbCol   = method.getAnnotation(DbCol.class);
            switch (dbCol.type()) {
                case INTEGER:
                    method.invoke(instance, row.getLong(column.getKey()));
                    break;
                case NUMERIC:
                    method.invoke(instance, row.getDouble(column.getKey()));
                    break;
                case DATE:
                    method.invoke(instance, row.getDate(column.getKey()));
                    break;
                default:
                    method.invoke(instance, row.getString(column.getKey()));
                    break;
            }
        }
        catch(Exception e) {
            if (!column.getKey().equalsIgnoreCase("ID"))
                log.error(String.format("Exception occurred while getting field '%s' value from recordset:%s", column, e.getMessage()));
        }
    }

    public static <T extends BaseEntity> T bind(Class<T> clazz, ResultSet row) {
        try {
            T instance = clazz.newInstance();
            Map<String, Method> methods = instance.getAnnotatedSetMethods();
            for (Map.Entry<String, Method> column : methods.entrySet())
                bindColumn1(instance, column, row);
            return instance;
        }
        catch(Exception ex) {
            log.error(String.format("Exception occurred while binding recordset with entity:%s", ex.getMessage()));
        }
        return null;
    }

    public void commit() {
        if (transaction != null && transaction.getStatus() == TransactionStatus.ACTIVE)
            transaction.commit();
    }

    public void rollback() {
        if (transaction != null && transaction.getStatus() == TransactionStatus.ACTIVE)
            transaction.rollback();
    }

    @Override
    public void close() {
        rollback();
        if (session != null) {
            session.disconnect();
            session.close();
            session = null;
        }
        transaction = null;
    }

    public static DataSource buildDataSource(String dbKey) {
        DriverManagerDataSource source = new DriverManagerDataSource();
        String prefix = dbKey+".datasource.%s";

        String driver   = PropertyUtil.getPropertyValue(String.format(prefix, "driver-class-name"), "");
        String userName = PropertyUtil.getPropertyValue(String.format(prefix, "username"), "");
        String password = PropertyUtil.getPropertyValue(String.format(prefix, "password"), "");
        String url      = PropertyUtil.getPropertyValue(String.format(prefix, "url"     ), "");

        source.setDriverClassName(driver);
        source.setUsername(userName);
        source.setPassword(password);
        source.setUrl     (url     );

        return source;
    }

    public static Parameter getParameter(Parameter[] parameters, String name) {
        for (Parameter p : parameters)
            if (p.getName().equals(name))
                return p;
        return null;
    }

    public void bindNamedParameters(PreparedStatement statement, String query, Parameter[] parameters) throws SQLException {
        List<String> queryParams = provider.getQueryParameters(query);
        int idx = 1;
        for (String p : queryParams) {
            Parameter parameter = getParameter(parameters, p);
            if (parameter != null)
                bindParameter(statement, parameter, idx++);
        }
    }

    public void bindNamedParameters(Query hQuery, String query, Parameter[] parameters) throws SQLException {
        List<String> queryParams = provider.getQueryParameters(query);
        int idx = 1;
        for (String p : queryParams) {
            Parameter parameter = getParameter(parameters, p);
            if (parameter != null)
                bindParameter(hQuery, parameter, idx++);
        }
    }

    public static final String INTEGER_NAME = "int"   ;
    public static final String BYTE_NAME    = "byte"  ;
    public static final String LONG_NAME    = "long"  ;
    public static final String FLOAT_NAME   = "float" ;
    public static final String DOUBLE_NAME  = "double";

    public static final String INTEGER_CLASS_NAME = "java.lang.Integer";
    public static final String BYTE_CLASS_NAME    = "java.lang.Byte"   ;
    public static final String LONG_CLASS_NAME    = "java.lang.Long"   ;
    public static final String FLOAT_CLASS_NAME   = "java.lang.Float"  ;
    public static final String DOUBLE_CLASS_NAME  = "java.lang.Double" ;
    public static final String STRING_CLASS_NAME  = "java.lang.String" ;
    public static final String DATE_CLASS_NAME    = "java.util.Date"   ;
    public static final String LIST_CLASS_NAME    = "java.util.ArrayList";

    static void bindParameter(Query query, Parameter parameter, int idx) {
        switch(parameter.getUnderlyingTypeName()) {
            case INTEGER_CLASS_NAME:
            case BYTE_CLASS_NAME:
            case LONG_CLASS_NAME:
                if (parameter.getValue() != null)
                    query.setLong(idx, MiscHelper.convertObjectToLong(parameter.getValue()));
                else
                    query.setLong(idx, 0);
                break;
            case FLOAT_CLASS_NAME:
            case DOUBLE_CLASS_NAME:
                if (parameter.getValue() != null)
                    query.setDouble(idx, MiscHelper.convertObjectToDouble(parameter.getValue()));
                else
                    query.setDouble(idx, 0);
                break;
            case DATE_CLASS_NAME:
                String dateValue = "";
                if (parameter.getValue() != null) {
                    Date date = MiscHelper.convertStringToDate(parameter.getValue().toString(), BaseEntity.DAFAULT_DATE_OUTPUT_FORMAT);
                    dateValue = MiscHelper.convertDateToString(date, BaseEntity.DAFAULT_ORACLE_DATE_FORMAT);
                }
                else
                    dateValue = MiscHelper.convertDateToString(new Date(), BaseEntity.DAFAULT_ORACLE_DATE_FORMAT);
                    query.setString(idx, dateValue);
                break;
            case LIST_CLASS_NAME:
                break;
            case STRING_CLASS_NAME:
            default:
                if (parameter.getValue() != null)
                    query.setString(idx, parameter.getValue().toString());
                else
                    query.setString(idx, "");
                break;
        }
    }

    public static void bindParameter(PreparedStatement statement, Parameter parameter, int idx) throws SQLException {
        switch(parameter.getUnderlyingTypeName()) {
            case INTEGER_CLASS_NAME:
            case BYTE_CLASS_NAME:
            case LONG_CLASS_NAME:
                if (parameter.getValue() != null)
                    statement.setLong(idx, MiscHelper.convertObjectToLong(parameter.getValue()));
                else
                    statement.setLong(idx, 0);
                break;
            case FLOAT_CLASS_NAME:
            case DOUBLE_CLASS_NAME:
                if (parameter.getValue() != null)
                    statement.setDouble(idx, MiscHelper.convertObjectToDouble(parameter.getValue()));
                else
                    statement.setDouble(idx, 0);
                break;
            case DATE_CLASS_NAME:
                String dateValue = "";
                if (parameter.getValue() != null) {
                    Date date = MiscHelper.convertStringToDate(parameter.getValue().toString(), BaseEntity.DAFAULT_DATE_OUTPUT_FORMAT);
                    dateValue = MiscHelper.convertDateToString(date, BaseEntity.DAFAULT_ORACLE_DATE_FORMAT);
                }
                else
                    dateValue = MiscHelper.convertDateToString(new Date(), BaseEntity.DAFAULT_ORACLE_DATE_FORMAT);
                statement.setString(idx, dateValue);
                break;
            case LIST_CLASS_NAME:
                break;
            case STRING_CLASS_NAME:
            default:
                if (parameter.getValue() != null)
                    statement.setString(idx, parameter.getValue().toString());
                else
                    statement.setString(idx, "");
                break;
        }
    }

    public static Type getHibernateType(Object value) {
        switch(value.getClass().getName()) {
            case INTEGER_CLASS_NAME:
            case BYTE_CLASS_NAME:
            case LONG_CLASS_NAME:
                return LongType.INSTANCE;
            case FLOAT_CLASS_NAME:
            case DOUBLE_CLASS_NAME:
                return DoubleType.INSTANCE;
            case DATE_CLASS_NAME:
                return DateType.INSTANCE;
            case STRING_CLASS_NAME:
            default:
                return StringType.INSTANCE;
        }
    }

    public static Map<String, Object> buildResultSetAsMap(ResultSet rs) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        for (int idx=1; idx<=rs.getMetaData().getColumnCount(); idx++) {
            String name = rs.getMetaData().getColumnName(idx);
            result.put(name, rs.getObject(name));
        }
        return result;
    }

    public class Parameter {
        private String name;
        private String addl;
        private Object value;
        private Type type;
        private boolean output = false;
        private RestQueryConfig.ParameterType parameterType = RestQueryConfig.ParameterType.SCALAR;

        public String getName () { return name;  }
        public Object getValue() { return value; }
        public Type   getType () { return type;  }
        public String getUnderlyingTypeName() { return value.getClass().getName(); }
        public String getNameAdditional    () { return addl;  }
        public boolean isOutput() { return output; }
        public RestQueryConfig.ParameterType getParameterType() { return parameterType; }

        public Parameter(String name, Object value) {
            this.name  = name;
            this.addl  = name;
            this.value = value;
            this.type  = Database.getHibernateType(value);
        }

        public Parameter(String name, Object value, Type type) {
            this(name, value);
            this.type = type;
        }

        public Parameter(String name, String nameAdditional, Type type, Object value, boolean output, RestQueryConfig.ParameterType parameterType) {
            this.name   = name;
            this.addl   = nameAdditional;
            this.type   = type;
            this.output = output;
            this.value  = value;
            this.parameterType = parameterType;

            if (output && type != LongType.INSTANCE && type != DoubleType.INSTANCE && type != StringType.INSTANCE)
                throw new IllegalArgumentException("Type can be either LongType or DoubleType or StringType for output parameter");
        }

        @Override
        public String toString() {
            return String.format("%s:%s:%s:%s", name, addl, parameterType.toString(), value);
        }
    }

    @FunctionalInterface
    public interface ValidateRecord {
        boolean process(ResultSet row, long currentRowIndex);
    }

    @FunctionalInterface
    public interface DataRecord {
        void process(Map<String, Object> row);
    }

    @FunctionalInterface
    public interface DataResultSet {
        void process(ResultSet row, long currentRowIndex);
    }
}
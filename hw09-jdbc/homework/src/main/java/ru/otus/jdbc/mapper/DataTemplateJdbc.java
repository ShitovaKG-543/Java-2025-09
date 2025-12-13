package ru.otus.jdbc.mapper;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.otus.core.repository.DataTemplate;
import ru.otus.core.repository.DataTemplateException;
import ru.otus.core.repository.executor.DbExecutor;

/** Сохраняет объект в базу, читает объект из базы */
@SuppressWarnings("java:S1068")
public class DataTemplateJdbc<T> implements DataTemplate<T> {

    private final DbExecutor dbExecutor;
    private final EntitySQLMetaData entitySQLMetaData;
    private final EntityClassMetaData<T> entityClassMetaData;

    public DataTemplateJdbc(
            DbExecutor dbExecutor, EntitySQLMetaData entitySQLMetaData, EntityClassMetaData<T> entityClassMetaData) {
        this.dbExecutor = dbExecutor;
        this.entitySQLMetaData = entitySQLMetaData;
        this.entityClassMetaData = entityClassMetaData;
    }

    @Override
    public Optional<T> findById(Connection connection, long id) {
        return dbExecutor.executeSelect(connection, entitySQLMetaData.getSelectByIdSql(), List.of(id), rs -> {
            try {
                if (rs.next()) {
                    return mapRow(rs);
                }
            } catch (SQLException e) {
                throw new DataTemplateException(e);
            }
            return null;
        });
    }

    @Override
    public List<T> findAll(Connection connection) {
        return dbExecutor
                .executeSelect(connection, entitySQLMetaData.getSelectAllSql(), List.of(), rs -> {
                    List<T> result = new ArrayList<>();
                    try {
                        while (rs.next()) {
                            result.add(mapRow(rs));
                        }
                        return result;
                    } catch (SQLException e) {
                        throw new DataTemplateException(e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("Unexpected error"));
    }

    @Override
    public long insert(Connection connection, T client) {
        try {
            List<Object> params = getParamsWithoutId(client);
            return dbExecutor.executeStatement(connection, entitySQLMetaData.getInsertSql(), params);
        } catch (Exception e) {
            throw new DataTemplateException(e);
        }
    }

    @Override
    public void update(Connection connection, T client) {
        try {
            List<Object> params = getParamsWithoutId(client);
            Object idValue = getIdValue(client);
            params.add(idValue);

            dbExecutor.executeStatement(connection, entitySQLMetaData.getUpdateSql(), params);
        } catch (Exception e) {
            throw new DataTemplateException(e);
        }
    }

    private T mapRow(ResultSet rs) {
        try {
            T instance = entityClassMetaData.getConstructor().newInstance();

            for (Field field : entityClassMetaData.getAllFields()) {
                Object value = rs.getObject(field.getName());
                field.set(instance, value);
            }

            return instance;
        } catch (Exception e) {
            throw new DataTemplateException(e);
        }
    }

    private List<Object> getParamsWithoutId(T object) throws IllegalAccessException {
        List<Object> params = new ArrayList<>();
        for (Field field : entityClassMetaData.getFieldsWithoutId()) {
            params.add(field.get(object));
        }
        return params;
    }

    private Object getIdValue(T object) throws IllegalAccessException {
        Field idField = entityClassMetaData.getIdField();
        return idField.get(object);
    }
}

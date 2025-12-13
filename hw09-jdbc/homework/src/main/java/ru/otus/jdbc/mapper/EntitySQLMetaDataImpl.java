package ru.otus.jdbc.mapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntitySQLMetaDataImpl implements EntitySQLMetaData {

    private final EntityClassMetaData<?> entityClassMetaData;

    @Override
    public String getSelectAllSql() {
        String tableName = entityClassMetaData.getName();
        return String.format("SELECT * FROM %s", tableName);
    }

    @Override
    public String getSelectByIdSql() {
        String tableName = entityClassMetaData.getName();
        String idColumn = entityClassMetaData.getIdField().getName();
        return String.format("SELECT * FROM %s WHERE %s = ?", tableName, idColumn);
    }

    @Override
    public String getInsertSql() {
        String tableName = entityClassMetaData.getName();
        List<Field> fieldsWithoutId = entityClassMetaData.getFieldsWithoutId();

        String columns = fieldsWithoutId.stream().map(Field::getName).collect(Collectors.joining(", "));

        String values = fieldsWithoutId.stream().map(f -> "?").collect(Collectors.joining(", "));

        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);
    }

    @Override
    public String getUpdateSql() {
        String tableName = entityClassMetaData.getName();
        Field idField = entityClassMetaData.getIdField();
        List<Field> fieldsWithoutId = entityClassMetaData.getFieldsWithoutId();

        String setClause =
                fieldsWithoutId.stream().map(field -> field.getName() + " = ?").collect(Collectors.joining(", "));

        return String.format("UPDATE %s SET %s WHERE %s = ?", tableName, setClause, idField.getName());
    }
}

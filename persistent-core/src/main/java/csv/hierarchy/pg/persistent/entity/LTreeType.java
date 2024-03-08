package csv.hierarchy.pg.persistent.entity;

import org.hibernate.HibernateException;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class LTreeType implements UserType<String> {
    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String s, String j1) throws HibernateException {
        return s.equals(j1);
    }

    @Override
    public int hashCode(String s) throws HibernateException {
        return s.hashCode();
    }

    @Override
    public String nullSafeGet(ResultSet resultSet, int i, SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws HibernateException, SQLException {
        return resultSet.getString(i);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, String s, int i, SharedSessionContractImplementor sharedSessionContractImplementor) throws SQLException {
        preparedStatement.setObject(i, s, Types.OTHER);
    }

    @Override
    public String deepCopy(String s) throws HibernateException {
        if (s == null)
            return null;
        if (!(s instanceof String))
            throw new IllegalStateException("Expected String, but got: " + s.getClass());
        return s;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String s) throws HibernateException {
        return (Serializable) s;
    }

    @Override
    public String assemble(Serializable serializable, Object o) throws HibernateException {
        return serializable.toString();
    }

    @Override
    public String replace(String s, String j1, Object o) {
        return deepCopy(s);
    }

//    @Override
//    public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
//        return UserType.super.getDefaultSqlLength(dialect, jdbcType);
//    }

//    @Override
//    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
//        return UserType.super.getDefaultSqlPrecision(dialect, jdbcType);
//    }

//    @Override
//    public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
//        return UserType.super.getDefaultSqlScale(dialect, jdbcType);
//    }

//    @Override
//    public JdbcType getJdbcType(TypeConfiguration typeConfiguration) {
//        return UserType.super.getJdbcType(typeConfiguration);
//    }

    @Override
    public BasicValueConverter<String, Object> getValueConverter() {
        return UserType.super.getValueConverter();
    }
}

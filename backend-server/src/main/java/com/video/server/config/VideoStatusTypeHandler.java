package com.video.server.config;

import com.video.server.constant.VideoStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * VideoStatus 枚举类型处理器
 * 数据库存储：0-待审核, 1-已发布, 2-驳回
 * Java 枚举：PENDING, PASSED, REJECTED
 */
@MappedTypes(VideoStatus.class)
@MappedJdbcTypes(JdbcType.TINYINT)
public class VideoStatusTypeHandler extends BaseTypeHandler<VideoStatus> {
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, VideoStatus parameter, JdbcType jdbcType) throws SQLException {
        int value;
        switch (parameter) {
            case PENDING:
                value = 0;
                break;
            case PASSED:
                value = 1;
                break;
            case REJECTED:
                value = 2;
                break;
            default:
                value = 0;
        }
        ps.setInt(i, value);
    }
    
    @Override
    public VideoStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return convertToEnum(value);
    }
    
    @Override
    public VideoStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return convertToEnum(value);
    }
    
    @Override
    public VideoStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return convertToEnum(value);
    }
    
    private VideoStatus convertToEnum(int value) {
        switch (value) {
            case 0:
                return VideoStatus.PENDING;
            case 1:
                return VideoStatus.PASSED;
            case 2:
                return VideoStatus.REJECTED;
            default:
                return VideoStatus.PENDING;
        }
    }
}

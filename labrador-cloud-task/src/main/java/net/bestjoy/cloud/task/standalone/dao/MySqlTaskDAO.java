package net.bestjoy.cloud.task.standalone.dao;

import net.bestjoy.cloud.task.TaskStatus;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 *
 */
@Mapper
public interface MySqlTaskDAO extends TaskDAO {


    @UpdateProvider(type = TaskSqlProvider.class, method = "batchMarkFreeTasksWithDomainMysql")
    @Override
    int batchMarkFreeTasksWithDomain(@Param("tableName") String tableName,
                                     @Param("batchId") String batchId,
                                     @Param("bizDomain") String bizDomain,
                                     @Param("n") int n,
                                     @Param("timeout") long timeout,
                                     @Param("failReloadTime") long failReloadTime,
                                     @Param("maxFailedCount") int maxFailedCount,
                                     @Param("delay") long delay);

    @Insert("INSERT ${tableName}(biz_domain, biz_id, failed_count, gmt_modified, gmt_create, status)" +
            "values (#{taskDO.bizDomain}, #{taskDO.bizId}, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, " + TaskStatus.NEW + ");")
    @Options(useGeneratedKeys = true)
    int insert(@Param("tableName") String tableName, @Param("taskDO") TaskDO taskDO);

    @Select("SELECT * FROM ${tableName} WHERE batch_id=#{batchId} ORDER BY id;")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "batch_id", property = "batchId"),
            @Result(column = "biz_id", property = "bizId"),
            @Result(column = "biz_domain", property = "bizDomain"),
            @Result(column = "failed_count", property = "failedCount"),
            @Result(column = "status", property = "status"),
            @Result(column = "gmt_modified", property = "gmtModified"),
            @Result(column = "gmt_create", property = "gmtCreate"),
    })
    List<TaskDO> selectByBatchId(@Param("tableName") String tableName,
                                 @Param("batchId") String batchId);

    @Update("UPDATE ${tableName} SET status=#{status}, failed_count=failed_count + #{failedCountInc} WHERE id=#{taskId};")
    int updateTaskStatus(@Param("tableName") String tableName,
                         @Param("taskId") long taskId,
                         @Param("status") int status,
                         @Param("failedCountInc") int failedCountInc);

    @Override
    @Delete("DELETE FROM ${tableName} WHERE id=#{taskId};")
    int remove(@Param("tableName") String tableName, @Param("taskId") long taskId);
}

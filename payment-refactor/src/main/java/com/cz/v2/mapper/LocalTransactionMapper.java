package com.cz.v2.mapper;




import com.cz.v2.entity.LocalTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author ThinkPad
* @description 针对表【local_transaction】的数据库操作Mapper
* @createDate 2025-05-08 09:58:04*/
@Mapper
public interface LocalTransactionMapper {

    void insertBatch(List<LocalTransaction> localList);

    List<LocalTransaction> selectByTimeRange(@Param("startTime") String startTime,
                                             @Param("endTime") String endTime,
                                             @Param("startIndex")int startIndex,
                                             @Param("pageSize")int pageSize);
}





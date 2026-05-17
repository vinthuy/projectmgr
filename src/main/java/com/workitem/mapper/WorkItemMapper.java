package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.entity.WorkItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WorkItemMapper extends BaseMapper<WorkItem> {
    
    List<Map<String, Object>> selectDynamicColumns(@Param("columns") List<String> columns, 
                                                    @Param("condition") Map<String, Object> condition,
                                                    @Param("page") int page,
                                                    @Param("pageSize") int pageSize);
    
    long countDynamicColumns(@Param("condition") Map<String, Object> condition);
}

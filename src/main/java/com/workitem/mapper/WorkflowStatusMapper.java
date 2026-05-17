package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.entity.WorkflowStatus;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowStatusMapper extends BaseMapper<WorkflowStatus> {
}

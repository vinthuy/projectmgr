package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.entity.WorkflowSchemeIssueType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkflowSchemeIssueTypeMapper extends BaseMapper<WorkflowSchemeIssueType> {
    
    @Select("SELECT wsit.*, wit.type_key as issueTypeKey, wit.type_name as issueTypeName, wit.icon as issueTypeIcon " +
            "FROM workflow_scheme_issue_type wsit " +
            "LEFT JOIN work_item_type wit ON wsit.issue_type_id = wit.id " +
            "WHERE wsit.scheme_id = #{schemeId} " +
            "ORDER BY wit.hierarchy_level ASC")
    List<WorkflowSchemeIssueType> selectWithDetails(Long schemeId);
    
    @Delete("DELETE FROM workflow_scheme_issue_type WHERE scheme_id = #{schemeId}")
    int deleteBySchemeId(Long schemeId);
}

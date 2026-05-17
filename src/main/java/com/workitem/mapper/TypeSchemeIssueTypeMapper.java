package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.entity.TypeSchemeIssueType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TypeSchemeIssueTypeMapper extends BaseMapper<TypeSchemeIssueType> {
    
    @Select("SELECT tsit.*, wit.type_key as issueTypeKey, wit.type_name as issueTypeName, wit.icon as issueTypeIcon " +
            "FROM type_scheme_issue_type tsit " +
            "LEFT JOIN work_item_type wit ON tsit.issue_type_id = wit.id " +
            "WHERE tsit.scheme_id = #{schemeId} " +
            "ORDER BY tsit.display_order ASC")
    List<TypeSchemeIssueType> selectWithDetails(Long schemeId);
    
    @Delete("DELETE FROM type_scheme_issue_type WHERE scheme_id = #{schemeId}")
    int deleteBySchemeId(Long schemeId);
}

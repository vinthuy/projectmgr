package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.dto.ScreenSchemeIssueTypeResponse;
import com.workitem.entity.ScreenSchemeIssueType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * ScreenSchemeIssueType Mapper接口
 */
@Mapper
public interface ScreenSchemeIssueTypeMapper extends BaseMapper<ScreenSchemeIssueType> {
    
    /**
     * 查询方案详情（含工作项类型和屏幕信息）
     */
    @Select("""
        SELECT ssit.*, 
               wit.type_key as issueTypeKey,
               wit.type_name as issueTypeName,
               wit.icon as issueTypeIcon,
               s1.screen_name as createScreenName,
               s2.screen_name as editScreenName,
               s3.screen_name as viewScreenName
        FROM screen_scheme_issue_type ssit
        LEFT JOIN work_item_type wit ON ssit.issue_type_id = wit.id
        LEFT JOIN screen s1 ON ssit.create_screen_id = s1.id
        LEFT JOIN screen s2 ON ssit.edit_screen_id = s2.id
        LEFT JOIN screen s3 ON ssit.view_screen_id = s3.id
        WHERE ssit.scheme_id = #{schemeId}
        ORDER BY wit.hierarchy_level, wit.type_name
        """)
    List<ScreenSchemeIssueTypeResponse> selectWithDetails(Long schemeId);
    
    /**
     * 删除方案的所有映射
     */
    @Delete("DELETE FROM screen_scheme_issue_type WHERE scheme_id = #{schemeId}")
    int deleteBySchemeId(Long schemeId);
}

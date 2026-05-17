package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.IssueLinkTypeCreateRequest;
import com.workitem.dto.IssueLinkTypeResponse;
import com.workitem.dto.IssueLinkTypeUpdateRequest;
import com.workitem.entity.IssueLinkType;
import com.workitem.mapper.IssueLinkTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueLinkTypeService extends ServiceImpl<IssueLinkTypeMapper, IssueLinkType> {

    /**
     * 获取所有关系类型
     */
    public List<IssueLinkTypeResponse> listAll(Long tenantId) {
        LambdaQueryWrapper<IssueLinkType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IssueLinkType::getTenantId, tenantId)
                .orderByAsc(IssueLinkType::getDisplayOrder);

        List<IssueLinkType> types = this.list(wrapper);

        return types.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取关系类型
     */
    public IssueLinkTypeResponse getById(Long id) {
        IssueLinkType type = this.baseMapper.selectById(id);
        if (type == null) {
            throw new RuntimeException("关系类型不存在: " + id);
        }
        return convertToResponse(type);
    }

    /**
     * 创建关系类型
     */
    @Transactional
    public IssueLinkTypeResponse create(Long tenantId, IssueLinkTypeCreateRequest request) {
        log.debug("开始创建关系类型, tenantId: {}, linkKey: {}", tenantId, request.getLinkKey());

        checkLinkKeyExists(tenantId, request.getLinkKey());

        IssueLinkType type = new IssueLinkType();
        type.setTenantId(tenantId);
        type.setLinkKey(request.getLinkKey());
        type.setInwardName(request.getInwardName());
        type.setOutwardName(request.getOutwardName());
        type.setDescription(request.getDescription());
        type.setIsSystem(false);
        type.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());

        this.save(type);
        log.debug("关系类型创建成功, id: {}, linkKey: {}", type.getId(), type.getLinkKey());
        return convertToResponse(type);
    }

    /**
     * 更新关系类型
     */
    @Transactional
    public IssueLinkTypeResponse update(Long id, IssueLinkTypeUpdateRequest request) {
        log.debug("开始更新关系类型, id: {}", id);

        IssueLinkType type = this.baseMapper.selectById(id);
        if (type == null) {
            throw new RuntimeException("关系类型不存在: " + id);
        }

        // 只更新非空字段
        if (request.getInwardName() != null) {
            type.setInwardName(request.getInwardName());
        }
        if (request.getOutwardName() != null) {
            type.setOutwardName(request.getOutwardName());
        }
        if (request.getDescription() != null) {
            type.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            type.setDisplayOrder(request.getDisplayOrder());
        }

        type.setUpdatedAt(LocalDateTime.now());
        this.updateById(type);

        log.debug("关系类型更新成功, id: {}", id);
        return convertToResponse(type);
    }

    /**
     * 删除关系类型（系统类型不允许删除）
     */
    @Transactional
    public void delete(Long id) {
        log.debug("开始删除关系类型, id: {}", id);

        IssueLinkType type = this.baseMapper.selectById(id);
        if (type == null) {
            throw new RuntimeException("关系类型不存在: " + id);
        }

        // 系统类型不允许删除
        if (type.getIsSystem() != null && type.getIsSystem()) {
            throw new RuntimeException("系统关系类型不允许删除");
        }

        this.removeById(id);
        log.debug("关系类型删除成功, id: {}", id);
    }

    /**
     * 检查关系标识是否已存在
     */
    private void checkLinkKeyExists(Long tenantId, String linkKey) {
        LambdaQueryWrapper<IssueLinkType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IssueLinkType::getTenantId, tenantId)
                .eq(IssueLinkType::getLinkKey, linkKey);

        if (this.count(wrapper) > 0) {
            throw new RuntimeException("关系标识已存在: " + linkKey);
        }
    }

    private IssueLinkTypeResponse convertToResponse(IssueLinkType type) {
        IssueLinkTypeResponse response = new IssueLinkTypeResponse();
        BeanUtils.copyProperties(type, response);
        return response;
    }
}

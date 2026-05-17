package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.IssueLinkCreateRequest;
import com.workitem.dto.IssueLinkResponse;
import com.workitem.entity.IssueLink;
import com.workitem.entity.IssueLinkType;
import com.workitem.entity.WorkItem;
import com.workitem.mapper.IssueLinkMapper;
import com.workitem.mapper.WorkItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueLinkService extends ServiceImpl<IssueLinkMapper, IssueLink> {

    private final IssueLinkTypeService linkTypeService;
    private final WorkItemService workItemService;
    private final WorkItemMapper workItemMapper;

    /**
     * 获取工作项的所有关系（双向查询）
     */
    public List<IssueLinkResponse> getLinksByItemId(Long tenantId, Long itemId) {
        log.debug("查询工作项关系, tenantId: {}, itemId: {}", tenantId, itemId);

        // 查询作为源和目标的所有关系
        LambdaQueryWrapper<IssueLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IssueLink::getTenantId, tenantId)
                .and(w -> w.eq(IssueLink::getSourceItemId, itemId)
                        .or()
                        .eq(IssueLink::getTargetItemId, itemId));

        List<IssueLink> links = this.list(wrapper);

        // 获取所有关系类型
        List<Long> typeIds = links.stream()
                .map(IssueLink::getLinkTypeId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, IssueLinkType> typeMap = typeIds.isEmpty() ? Map.of() :
                linkTypeService.listByIds(typeIds).stream()
                        .collect(Collectors.toMap(IssueLinkType::getId, t -> t));

        // 获取所有关联的工作项ID
        List<Long> relatedItemIds = links.stream()
                .map(link -> link.getSourceItemId().equals(itemId) ?
                        link.getTargetItemId() : link.getSourceItemId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, WorkItem> itemMap = relatedItemIds.isEmpty() ? Map.of() :
                workItemService.listByIds(relatedItemIds).stream()
                        .collect(Collectors.toMap(WorkItem::getId, i -> i));

        // 转换为响应对象
        List<IssueLinkResponse> responses = new ArrayList<>();
        for (IssueLink link : links) {
            IssueLinkType linkType = typeMap.get(link.getLinkTypeId());
            if (linkType == null) continue;

            boolean isSource = link.getSourceItemId().equals(itemId);
            Long relatedItemId = isSource ? link.getTargetItemId() : link.getSourceItemId();
            WorkItem relatedItem = itemMap.get(relatedItemId);

            IssueLinkResponse response = new IssueLinkResponse();
            response.setId(link.getId());
            response.setLinkTypeKey(linkType.getLinkKey());
            response.setLinkTypeName(isSource ? linkType.getOutwardName() : linkType.getInwardName());
            response.setDirection(isSource ? "outward" : "inward");
            response.setRelatedItemId(relatedItemId);
            response.setRelatedItemKey(relatedItem != null ? relatedItem.getIssueKey() : null);
            response.setRelatedItemSummary(relatedItem != null ? relatedItem.getTitle() : null);
            response.setComment(link.getComment());
            response.setCreatedBy(link.getCreatedBy());
            response.setCreatedAt(link.getCreatedAt());

            responses.add(response);
        }

        log.debug("查询到 {} 个关系", responses.size());
        return responses;
    }

    /**
     * 创建工作项关系
     */
    @Transactional
    public IssueLinkResponse create(Long tenantId, Long userId, IssueLinkCreateRequest request) {
        log.debug("开始创建工作项关系, tenantId: {}, source: {}, target: {}",
                tenantId, request.getSourceItemId(), request.getTargetItemId());

        // 验证不能自关联
        if (request.getSourceItemId().equals(request.getTargetItemId())) {
            throw new RuntimeException("工作项不能与自己建立关系");
        }

        // 验证关系类型存在
        List<IssueLinkType> types = linkTypeService.lambdaQuery()
                .eq(IssueLinkType::getTenantId, tenantId)
                .eq(IssueLinkType::getLinkKey, request.getLinkTypeKey())
                .list();

        if (types.isEmpty()) {
            throw new RuntimeException("关系类型不存在: " + request.getLinkTypeKey());
        }

        IssueLinkType linkType = types.get(0);

        // 检查关系是否已存在
        checkLinkExists(tenantId, request.getSourceItemId(),
                request.getTargetItemId(), linkType.getId());

        // 验证工作项存在
        WorkItem sourceItem = workItemMapper.selectById(request.getSourceItemId());
        WorkItem targetItem = workItemMapper.selectById(request.getTargetItemId());

        if (sourceItem == null || targetItem == null) {
            throw new RuntimeException("工作项不存在");
        }

        // 创建关系
        IssueLink link = new IssueLink();
        link.setTenantId(tenantId);
        link.setLinkTypeId(linkType.getId());
        link.setSourceItemId(request.getSourceItemId());
        link.setTargetItemId(request.getTargetItemId());
        link.setComment(request.getComment());
        link.setCreatedBy(userId);
        link.setCreatedAt(LocalDateTime.now());
        link.setUpdatedAt(LocalDateTime.now());

        this.save(link);

        log.debug("工作项关系创建成功, id: {}", link.getId());

        // 构建响应
        IssueLinkResponse response = new IssueLinkResponse();
        response.setId(link.getId());
        response.setLinkTypeKey(linkType.getLinkKey());
        response.setLinkTypeName(linkType.getOutwardName());
        response.setDirection("outward");
        response.setRelatedItemId(targetItem.getId());
        response.setRelatedItemKey(targetItem.getIssueKey());
        response.setRelatedItemSummary(targetItem.getTitle());
        response.setComment(link.getComment());
        response.setCreatedBy(link.getCreatedBy());
        response.setCreatedAt(link.getCreatedAt());

        return response;
    }

    /**
     * 删除工作项关系
     */
    @Transactional
    public void delete(Long id) {
        log.debug("开始删除工作项关系, id: {}", id);

        IssueLink link = this.baseMapper.selectById(id);
        if (link == null) {
            throw new RuntimeException("工作项关系不存在: " + id);
        }

        this.removeById(id);
        log.debug("工作项关系删除成功, id: {}", id);
    }

    /**
     * 检查关系是否存在
     */
    private void checkLinkExists(Long tenantId, Long sourceItemId,
                                  Long targetItemId, Long linkTypeId) {
        LambdaQueryWrapper<IssueLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IssueLink::getTenantId, tenantId)
                .eq(IssueLink::getSourceItemId, sourceItemId)
                .eq(IssueLink::getTargetItemId, targetItemId)
                .eq(IssueLink::getLinkTypeId, linkTypeId);

        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该关系已存在");
        }
    }
}

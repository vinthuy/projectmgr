package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.*;
import com.workitem.entity.*;
import com.workitem.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Screen服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenService extends ServiceImpl<ScreenMapper, Screen> {

    private final ScreenTabMapper screenTabMapper;
    private final ScreenItemMapper screenItemMapper;
    private final FieldDefinitionMapper fieldDefinitionMapper;
    private final IssueTypeScreenMapper issueTypeScreenMapper;
    private final WorkItemTypeMapper workItemTypeMapper;

    /**
     * 获取所有Screen列表
     */
    public List<ScreenResponse> listAll(Long tenantId) {
        log.debug("查询所有Screen, tenantId: {}", tenantId);
        
        LambdaQueryWrapper<Screen> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Screen::getTenantId, tenantId)
                .orderByDesc(Screen::getCreatedAt);
        
        List<Screen> screens = this.list(wrapper);
        
        return screens.stream()
                .map(this::convertToSimpleResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取Screen详情（包含Tab和字段）
     */
    public ScreenResponse getById(Long id) {
        log.debug("查询Screen详情, id: {}", id);
        
        Screen screen = this.baseMapper.selectById(id);
        if (screen == null) {
            throw new RuntimeException("Screen不存在: " + id);
        }
        
        return convertToDetailResponse(screen);
    }

    /**
     * 创建Screen
     */
    @Transactional
    public ScreenResponse create(Long tenantId, ScreenCreateRequest request) {
        log.debug("开始创建Screen, tenantId: {}, screenName: {}", tenantId, request.getScreenName());
        
        // 检查名称唯一性
        checkScreenNameExists(tenantId, request.getScreenName());
        
        // 创建Screen
        Screen screen = new Screen();
        screen.setTenantId(tenantId);
        screen.setScreenName(request.getScreenName());
        screen.setDescription(request.getDescription());
        screen.setIsSystem(false);
        screen.setCreatedAt(LocalDateTime.now());
        screen.setUpdatedAt(LocalDateTime.now());
        
        this.save(screen);
        
        // 自动创建默认Tab
        ScreenTab defaultTab = new ScreenTab();
        defaultTab.setScreenId(screen.getId());
        defaultTab.setTabName("详情");
        defaultTab.setDisplayOrder(0);
        defaultTab.setCreatedAt(LocalDateTime.now());
        defaultTab.setUpdatedAt(LocalDateTime.now());
        screenTabMapper.insert(defaultTab);
        
        // 如果提供了初始字段，添加到默认Tab
        if (request.getFieldIds() != null && !request.getFieldIds().isEmpty()) {
            for (int i = 0; i < request.getFieldIds().size(); i++) {
                ScreenItem item = new ScreenItem();
                item.setScreenId(screen.getId());
                item.setScreenTabId(defaultTab.getId());
                item.setFieldDefinitionId(request.getFieldIds().get(i));
                item.setDisplayOrder(i);
                item.setCreatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                screenItemMapper.insert(item);
            }
        }
        
        log.debug("Screen创建成功, id: {}", screen.getId());
        return convertToDetailResponse(screen);
    }

    /**
     * 更新Screen基本信息
     */
    @Transactional
    public ScreenResponse update(Long id, ScreenUpdateRequest request) {
        log.debug("开始更新Screen, id: {}", id);
        
        Screen screen = this.baseMapper.selectById(id);
        if (screen == null) {
            throw new RuntimeException("Screen不存在: " + id);
        }
        
        // 系统Screen不允许修改名称
        if (screen.getIsSystem() && request.getScreenName() != null) {
            throw new RuntimeException("系统Screen不允许修改名称");
        }
        
        if (request.getScreenName() != null) {
            checkScreenNameExists(screen.getTenantId(), request.getScreenName(), id);
            screen.setScreenName(request.getScreenName());
        }
        
        if (request.getDescription() != null) {
            screen.setDescription(request.getDescription());
        }
        
        screen.setUpdatedAt(LocalDateTime.now());
        this.updateById(screen);
        
        log.debug("Screen更新成功, id: {}", id);
        return convertToDetailResponse(screen);
    }

    /**
     * 删除Screen
     */
    @Transactional
    public void delete(Long id) {
        log.debug("开始删除Screen, id: {}", id);
        
        Screen screen = this.baseMapper.selectById(id);
        if (screen == null) {
            throw new RuntimeException("Screen不存在: " + id);
        }
        
        // 系统Screen不允许删除
        if (screen.getIsSystem()) {
            throw new RuntimeException("系统Screen不允许删除");
        }
        
        // TODO: 检查是否有Issue Type在使用此Screen
        
        this.removeById(id);
        log.debug("Screen删除成功, id: {}", id);
    }

    /**
     * 添加Tab
     */
    @Transactional
    public ScreenTabResponse addTab(Long screenId, String tabName) {
        log.debug("开始添加Tab, screenId: {}, tabName: {}", screenId, tabName);
        
        Screen screen = this.baseMapper.selectById(screenId);
        if (screen == null) {
            throw new RuntimeException("Screen不存在: " + screenId);
        }
        
        // 获取当前最大displayOrder
        LambdaQueryWrapper<ScreenTab> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScreenTab::getScreenId, screenId)
                .orderByDesc(ScreenTab::getDisplayOrder)
                .last("LIMIT 1");
        ScreenTab lastTab = screenTabMapper.selectOne(wrapper);
        
        int displayOrder = (lastTab != null ? lastTab.getDisplayOrder() : -1) + 1;
        
        ScreenTab tab = new ScreenTab();
        tab.setScreenId(screenId);
        tab.setTabName(tabName);
        tab.setDisplayOrder(displayOrder);
        tab.setCreatedAt(LocalDateTime.now());
        tab.setUpdatedAt(LocalDateTime.now());
        
        screenTabMapper.insert(tab);
        
        log.debug("Tab添加成功, id: {}", tab.getId());
        return convertToTabResponse(tab, Collections.emptyList());
    }

    /**
     * 更新Tab
     */
    @Transactional
    public void updateTab(Long tabId, String tabName) {
        log.debug("开始更新Tab, tabId: {}", tabId);
        
        ScreenTab tab = screenTabMapper.selectById(tabId);
        if (tab == null) {
            throw new RuntimeException("Tab不存在: " + tabId);
        }
        
        tab.setTabName(tabName);
        tab.setUpdatedAt(LocalDateTime.now());
        screenTabMapper.updateById(tab);
        
        log.debug("Tab更新成功, tabId: {}", tabId);
    }

    /**
     * 删除Tab
     */
    @Transactional
    public void deleteTab(Long tabId) {
        log.debug("开始删除Tab, tabId: {}", tabId);
        
        ScreenTab tab = screenTabMapper.selectById(tabId);
        if (tab == null) {
            throw new RuntimeException("Tab不存在: " + tabId);
        }
        
        // 删除Tab下的所有ScreenItem
        LambdaQueryWrapper<ScreenItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScreenItem::getScreenTabId, tabId);
        screenItemMapper.delete(wrapper);
        
        // 删除Tab
        screenTabMapper.deleteById(tabId);
        
        log.debug("Tab删除成功, tabId: {}", tabId);
    }

    /**
     * 调整Tab顺序
     */
    @Transactional
    public void reorderTabs(Long screenId, List<Long> tabIds) {
        log.debug("开始调整Tab顺序, screenId: {}", screenId);
        
        for (int i = 0; i < tabIds.size(); i++) {
            ScreenTab tab = screenTabMapper.selectById(tabIds.get(i));
            if (tab != null && tab.getScreenId().equals(screenId)) {
                tab.setDisplayOrder(i);
                tab.setUpdatedAt(LocalDateTime.now());
                screenTabMapper.updateById(tab);
            }
        }
        
        log.debug("Tab顺序调整成功");
    }

    /**
     * 添加字段到Screen
     */
    @Transactional
    public ScreenItemResponse addField(Long screenId, AddFieldToScreenRequest request) {
        log.debug("开始添加字段到Screen, screenId: {}, fieldId: {}", screenId, request.getFieldDefinitionId());
        
        Screen screen = this.baseMapper.selectById(screenId);
        if (screen == null) {
            throw new RuntimeException("Screen不存在: " + screenId);
        }
        
        // 检查字段是否已存在
        LambdaQueryWrapper<ScreenItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScreenItem::getScreenId, screenId)
                .eq(ScreenItem::getFieldDefinitionId, request.getFieldDefinitionId());
        if (screenItemMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("字段已存在于该Screen中");
        }
        
        // 确定Tab
        Long tabId = request.getScreenTabId();
        if (tabId == null) {
            // 使用第一个Tab
            LambdaQueryWrapper<ScreenTab> tabWrapper = new LambdaQueryWrapper<>();
            tabWrapper.eq(ScreenTab::getScreenId, screenId)
                    .orderByAsc(ScreenTab::getDisplayOrder)
                    .last("LIMIT 1");
            ScreenTab firstTab = screenTabMapper.selectOne(tabWrapper);
            if (firstTab != null) {
                tabId = firstTab.getId();
            }
        }
        
        // 确定displayOrder
        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            LambdaQueryWrapper<ScreenItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(ScreenItem::getScreenId, screenId)
                    .eq(ScreenItem::getScreenTabId, tabId)
                    .orderByDesc(ScreenItem::getDisplayOrder)
                    .last("LIMIT 1");
            ScreenItem lastItem = screenItemMapper.selectOne(itemWrapper);
            displayOrder = (lastItem != null ? lastItem.getDisplayOrder() : -1) + 1;
        }
        
        // 创建ScreenItem
        ScreenItem item = new ScreenItem();
        item.setScreenId(screenId);
        item.setScreenTabId(tabId);
        item.setFieldDefinitionId(request.getFieldDefinitionId());
        item.setDisplayOrder(displayOrder);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        
        screenItemMapper.insert(item);
        
        log.debug("字段添加成功, itemId: {}", item.getId());
        return convertToItemResponse(item);
    }

    /**
     * 从Screen移除字段
     */
    @Transactional
    public void removeField(Long itemId) {
        log.debug("开始移除字段, itemId: {}", itemId);
        
        ScreenItem item = screenItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("ScreenItem不存在: " + itemId);
        }
        
        screenItemMapper.deleteById(itemId);
        log.debug("字段移除成功, itemId: {}", itemId);
    }

    /**
     * 调整字段顺序
     */
    @Transactional
    public void reorderFields(Long screenId, List<Long> itemIds) {
        log.debug("开始调整字段顺序, screenId: {}", screenId);
        
        for (int i = 0; i < itemIds.size(); i++) {
            ScreenItem item = screenItemMapper.selectById(itemIds.get(i));
            if (item != null && item.getScreenId().equals(screenId)) {
                item.setDisplayOrder(i);
                item.setUpdatedAt(LocalDateTime.now());
                screenItemMapper.updateById(item);
            }
        }
        
        log.debug("字段顺序调整成功");
    }

    // ==================== 私有方法 ====================

    private void checkScreenNameExists(Long tenantId, String screenName) {
        checkScreenNameExists(tenantId, screenName, null);
    }

    private void checkScreenNameExists(Long tenantId, String screenName, Long excludeId) {
        LambdaQueryWrapper<Screen> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Screen::getTenantId, tenantId)
                .eq(Screen::getScreenName, screenName);
        if (excludeId != null) {
            wrapper.ne(Screen::getId, excludeId);
        }
        
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("Screen名称已存在: " + screenName);
        }
    }

    private ScreenResponse convertToSimpleResponse(Screen screen) {
        ScreenResponse response = new ScreenResponse();
        BeanUtils.copyProperties(screen, response);
        return response;
    }

    private ScreenResponse convertToDetailResponse(Screen screen) {
        ScreenResponse response = convertToSimpleResponse(screen);
        
        // 查询Tabs
        LambdaQueryWrapper<ScreenTab> tabWrapper = new LambdaQueryWrapper<>();
        tabWrapper.eq(ScreenTab::getScreenId, screen.getId())
                .orderByAsc(ScreenTab::getDisplayOrder);
        List<ScreenTab> tabs = screenTabMapper.selectList(tabWrapper);
        
        List<ScreenTabResponse> tabResponses = new ArrayList<>();
        for (ScreenTab tab : tabs) {
            // 查询Tab下的字段
            LambdaQueryWrapper<ScreenItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(ScreenItem::getScreenTabId, tab.getId())
                    .orderByAsc(ScreenItem::getDisplayOrder);
            List<ScreenItem> items = screenItemMapper.selectList(itemWrapper);
            
            List<ScreenItemResponse> itemResponses = items.stream()
                    .map(this::convertToItemResponse)
                    .collect(Collectors.toList());
            
            tabResponses.add(convertToTabResponse(tab, itemResponses));
        }
        
        response.setTabs(tabResponses);
        
        // 查询Issue Type映射
        LambdaQueryWrapper<IssueTypeScreen> itsWrapper = new LambdaQueryWrapper<>();
        itsWrapper.eq(IssueTypeScreen::getScreenId, screen.getId());
        List<IssueTypeScreen> mappings = issueTypeScreenMapper.selectList(itsWrapper);
        
        Map<String, String> issueTypeMappings = new HashMap<>();
        for (IssueTypeScreen mapping : mappings) {
            WorkItemType workItemType = workItemTypeMapper.selectById(mapping.getWorkItemTypeId());
            if (workItemType != null) {
                issueTypeMappings.put(mapping.getOperationType(), workItemType.getTypeName());
            }
        }
        response.setIssueTypeMappings(issueTypeMappings);
        
        return response;
    }

    private ScreenTabResponse convertToTabResponse(ScreenTab tab, List<ScreenItemResponse> items) {
        ScreenTabResponse response = new ScreenTabResponse();
        response.setId(tab.getId());
        response.setTabName(tab.getTabName());
        response.setDisplayOrder(tab.getDisplayOrder());
        response.setItems(items);
        return response;
    }

    private ScreenItemResponse convertToItemResponse(ScreenItem item) {
        ScreenItemResponse response = new ScreenItemResponse();
        response.setId(item.getId());
        response.setFieldDefinitionId(item.getFieldDefinitionId());
        response.setScreenTabId(item.getScreenTabId());  // 设置 Tab ID
        response.setDisplayOrder(item.getDisplayOrder());
        
        // 查询字段信息
        FieldDefinition field = fieldDefinitionMapper.selectById(item.getFieldDefinitionId());
        if (field != null) {
            response.setFieldKey(field.getFieldKey());
            response.setFieldName(field.getFieldName());
            response.setFieldType(field.getFieldType());
        }
        
        return response;
    }
}

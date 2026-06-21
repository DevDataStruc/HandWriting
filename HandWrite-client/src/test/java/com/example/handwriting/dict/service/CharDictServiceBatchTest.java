package com.example.handwriting.dict.service;

import com.example.handwriting.dict.dto.CharDictBatchCreateDTO;
import com.example.handwriting.dict.dto.CharDictImportResultVO;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.repository.CharDictRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CharDictService 批量导入逻辑单元测试
 *
 * <p>覆盖场景：</p>
 * <ul>
 *   <li>空集合 → 返回 message 提示</li>
 *   <li>去重：已存在字符应被跳过，计入 skipped 而非 failed</li>
 *   <li>默认值兜底：item 缺字段时回退到 dto 顶层默认值</li>
 *   <li>超长字符（>8）→ 计入 failed</li>
 *   <li>实际新增数 = 写入 saveAll 的集合大小</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CharDictServiceBatchTest {

    @Mock
    private CharDictRepository repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private CharDictService service;

    @BeforeEach
    void setup() {
        // 使用 lenient 避免「未使用 stub」报错（部分测试不需要此桩）
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("空 items 时返回提示，不调用 repository")
    void emptyItems() {
        CharDictImportResultVO vo = service.batchCreateWithDefaults(new CharDictBatchCreateDTO(), new ArrayList<>());
        assertNotNull(vo);
        assertEquals(0, vo.getInserted());
        assertEquals(0, vo.getSkipped());
        assertEquals(0, vo.getFailed());
        verify(repository, never()).saveAll(anyCollection());
    }

    @Test
    @DisplayName("重复字符应跳过，幂等")
    void duplicatesAreSkipped() {
        // 模拟已存在
        Set<String> existing = new HashSet<>(Arrays.asList("永", "的"));
        when(repository.existsByCharValue(anyString()))
                .thenAnswer(inv -> existing.contains((String) inv.getArgument(0)));
        when(repository.saveAll(anyCollection()))
                .thenAnswer(inv -> {
                    List<CharDict> arg = inv.getArgument(0);
                    long id = 1;
                    for (CharDict c : arg) c.setId(id++);
                    return new ArrayList<>(arg);
                });

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        dto.setDefaultCategory("HANZI");
        dto.setDefaultDifficulty(1);
        dto.setDefaultEnabled(1);

        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        items.add(item("永"));   // 已存在
        items.add(item("的"));   // 已存在
        items.add(item("我"));   // 新增
        items.add(item("你"));   // 新增

        CharDictImportResultVO vo = service.batchCreateWithDefaults(dto, items);
        assertEquals(2, vo.getInserted(), "应新增 2 条");
        assertEquals(2, vo.getSkipped(), "应跳过 2 条已存在");
        assertEquals(0, vo.getFailed());

        ArgumentCaptor<List<CharDict>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).saveAll(captor.capture());
        List<CharDict> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(c -> "我".equals(c.getCharValue())));
        assertTrue(saved.stream().anyMatch(c -> "你".equals(c.getCharValue())));
    }

    @Test
    @DisplayName("缺省字段回退到 dto 默认值")
    void defaultsFallback() {
        when(repository.existsByCharValue(anyString())).thenReturn(false);
        when(repository.saveAll(anyCollection()))
                .thenAnswer(inv -> {
                    List<CharDict> arg = inv.getArgument(0);
                    long id = 1;
                    for (CharDict c : arg) c.setId(id++);
                    return new ArrayList<>(arg);
                });

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        dto.setDefaultCategory("LETTER");
        dto.setDefaultDifficulty(4);
        dto.setDefaultDescription("默认描述");
        dto.setDefaultEnabled(0);

        // 全部 item 缺字段
        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        items.add(item("A"));
        items.add(item("B"));

        CharDictImportResultVO vo = service.batchCreateWithDefaults(dto, items);
        assertEquals(2, vo.getInserted());

        ArgumentCaptor<List<CharDict>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        for (CharDict c : captor.getValue()) {
            assertEquals("LETTER", c.getCategory());
            assertEquals(4, c.getDifficulty());
            assertEquals("默认描述", c.getDescription());
            assertEquals(0, c.getEnabled());
        }
    }

    @Test
    @DisplayName("超长字符（>8）视为失败")
    void longCharValueFails() {
        when(repository.existsByCharValue(anyString())).thenReturn(false);
        when(repository.saveAll(anyCollection()))
                .thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        dto.setDefaultCategory("HANZI");
        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        items.add(item("正常"));
        items.add(item("一整段超长字符串大于8个字符应该被拒绝"));

        CharDictImportResultVO vo = service.batchCreateWithDefaults(dto, items);
        assertEquals(1, vo.getInserted());
        assertEquals(1, vo.getFailed());
        assertNotNull(vo.getFailedSamples());
        assertTrue(vo.getFailedSamples().stream()
                .anyMatch(s -> s.startsWith("一整段超长字符串")));
    }

    @Test
    @DisplayName("默认分类兜底为 HANZI")
    void defaultCategoryFallback() {
        when(repository.existsByCharValue(anyString())).thenReturn(false);
        when(repository.saveAll(anyCollection()))
                .thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        // 不设置 defaultCategory
        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        items.add(item("字"));

        CharDictImportResultVO vo = service.batchCreateWithDefaults(dto, items);
        assertEquals(1, vo.getInserted());

        ArgumentCaptor<List<CharDict>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals("HANZI", captor.getValue().get(0).getCategory());
    }

    @Test
    @DisplayName("当至少新增一条时清空缓存")
    void cacheClearedWhenInserted() {
        when(repository.existsByCharValue(anyString())).thenReturn(false);
        when(repository.saveAll(anyCollection()))
                .thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        items.add(item("字"));

        service.batchCreateWithDefaults(dto, items);
        // 至少应清掉 5 个分类（ALL/HANZI/DIGIT/LETTER/SYMBOL）
        verify(redisTemplate, atLeastOnce()).delete(anyString());
    }

    private CharDictBatchCreateDTO.Item item(String value) {
        CharDictBatchCreateDTO.Item it = new CharDictBatchCreateDTO.Item();
        it.setCharValue(value);
        it.setLineNo(1);
        return it;
    }
}

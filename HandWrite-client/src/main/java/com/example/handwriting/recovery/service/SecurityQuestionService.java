package com.example.handwriting.recovery.service;

import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.recovery.dto.SecurityQuestionVO;
import com.example.handwriting.recovery.dto.SecurityQuestionItem;
import com.example.handwriting.recovery.dto.SetupSecurityQuestionsDTO;
import com.example.handwriting.recovery.entity.UserSecurityQuestion;
import com.example.handwriting.recovery.repository.UserSecurityQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 密保问题服务
 *  - 答案在入库前先 trim + toLowerCase(Locale.ROOT) + 去除所有空白
 *  - 哈希时拼接每题独立 salt 后用 BCrypt 编码
 *  - 校验时同样归一化后比对
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityQuestionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SALT_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final UserSecurityQuestionRepository repository;
    private final PasswordEncoder passwordEncoder;

    /** 预置问题 key → 默认文案（用户可改写） */
    public static final java.util.Map<String, String> PRESET_QUESTIONS = java.util.Map.of(
            "favoriteTeacher", "你小学时最喜欢的老师姓什么？",
            "firstPet", "你的第一只宠物叫什么？",
            "bornCity", "你出生在哪个城市？",
            "motherName", "你母亲的姓名是？",
            "firstSchool", "你就读的第一所学校名称是？",
            "childhoodNickname", "你的童年昵称是？",
            "favoriteMovie", "你最喜欢的一部电影是？",
            "favoriteBook", "你最喜欢的一本书是？"
    );

    /** 当前用户的密保问题列表（仅返回元信息，不含答案） */
    public List<SecurityQuestionVO> listByUser(Long userId) {
        List<UserSecurityQuestion> entities = repository.findByUserIdOrderByQuestionIndexAsc(userId);
        List<SecurityQuestionVO> result = new ArrayList<>();
        for (UserSecurityQuestion e : entities) {
            SecurityQuestionVO vo = new SecurityQuestionVO();
            vo.setQuestionIndex(e.getQuestionIndex());
            vo.setQuestionKey(e.getQuestionKey());
            vo.setQuestionText(e.getQuestionText());
            result.add(vo);
        }
        return result;
    }

    /** 是否已设置 */
    public boolean isSet(Long userId) {
        return repository.countByUserId(userId) >= 3;
    }

    /**
     * 一次性设置 3 题
     *  - 已存在则全部覆盖（前端提示"覆盖将清空原密保"）
     */
    @Transactional
    public void setup(Long userId, SetupSecurityQuestionsDTO dto) {
        List<SecurityQuestionItem> items = dto.getQuestions();
        if (items == null || items.size() != 3) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        // 同一份设置中问题 key 不可重复
        Set<String> keys = new HashSet<>();
        for (SecurityQuestionItem q : items) {
            if (!keys.add(q.getQuestionKey())) {
                throw new BizException(ErrorCode.BAD_REQUEST);
            }
        }
        repository.deleteByUserId(userId);
        List<UserSecurityQuestion> entities = new ArrayList<>(3);
        for (int i = 0; i < items.size(); i++) {
            SecurityQuestionItem item = items.get(i);
            UserSecurityQuestion e = new UserSecurityQuestion();
            e.setUserId(userId);
            e.setQuestionIndex(i + 1);
            e.setQuestionKey(item.getQuestionKey());
            e.setQuestionText(item.getQuestionText());
            String salt = randomSalt(16);
            e.setSalt(salt);
            e.setAnswerHash(passwordEncoder.encode(normalizeAnswer(item.getAnswer()) + ":" + salt));
            entities.add(e);
        }
        repository.saveAll(entities);
    }

    /** 清空当前用户所有密保 */
    @Transactional
    public void clear(Long userId) {
        repository.deleteByUserId(userId);
    }

    /**
     * 校验用户提交的 3 道答案
     *  - 全部正确才返回 true
     *  - 任一错误抛 SECURITY_QUESTION_ANSWER_INVALID
     */
    @Transactional
    public void verifyAll(Long userId, List<com.example.handwriting.recovery.dto.SecurityAnswerItem> answers) {
        if (answers == null || answers.size() != 3) {
            throw new BizException(ErrorCode.SECURITY_QUESTION_ANSWER_INVALID);
        }
        List<UserSecurityQuestion> entities = repository.findByUserIdOrderByQuestionIndexAsc(userId);
        if (entities.size() != 3) {
            throw new BizException(ErrorCode.SECURITY_QUESTION_NOT_SET);
        }
        for (var ans : answers) {
            UserSecurityQuestion target = entities.stream()
                    .filter(e -> e.getQuestionIndex().equals(ans.getQuestionIndex()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(ErrorCode.SECURITY_QUESTION_ANSWER_INVALID));
            String candidate = normalizeAnswer(ans.getAnswer()) + ":" + target.getSalt();
            if (!passwordEncoder.matches(candidate, target.getAnswerHash())) {
                throw new BizException(ErrorCode.SECURITY_QUESTION_ANSWER_INVALID);
            }
        }
    }

    /** 归一化：去首尾空白、统一小写、去除所有内部空白字符 */
    private static String normalizeAnswer(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String randomSalt(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SALT_ALPHABET.charAt(RANDOM.nextInt(SALT_ALPHABET.length())));
        }
        return sb.toString();
    }
}

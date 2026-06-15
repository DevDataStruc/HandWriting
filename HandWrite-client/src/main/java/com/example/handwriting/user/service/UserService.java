package com.example.handwriting.user.service;

import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.user.dto.UserProfileDTO;
import com.example.handwriting.user.dto.UserVO;
import com.example.handwriting.user.entity.User;
import com.example.handwriting.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserVO getProfile(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_EXISTS));
        return toVO(u);
    }

    @Transactional
    public UserVO updateProfile(Long userId, UserProfileDTO dto) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_EXISTS));
        if (dto.getNickname() != null) u.setNickname(dto.getNickname());
        if (dto.getEmail() != null) u.setEmail(dto.getEmail());
        if (dto.getPhone() != null) u.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) u.setAvatar(dto.getAvatar());
        userRepository.save(u);
        return toVO(u);
    }

    public UserVO toVO(User u) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(u, vo);
        return vo;
    }
}

package com.geollm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.geollm.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}


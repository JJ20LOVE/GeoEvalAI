package com.geollm.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geollm.entity.Class;
import com.geollm.mapper.ClassMapper;
import com.geollm.service.ClassService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {
    private final ClassMapper classMapper;

    public ClassServiceImpl(ClassMapper classMapper) {
        this.classMapper = classMapper;
    }

    @Override
    public List<Class> getAll() {
        return classMapper.selectList(null);
    }

    @Override
    public int add(String className) {
        Long cnt = classMapper.selectCount(new LambdaQueryWrapper<Class>()
                .eq(Class::getClassName, className));
        if (cnt != null && cnt > 0) return 503;
        Class c = new Class();
        c.setClassName(className);
        classMapper.insert(c);
        return 200;
    }

    @Override
    public int update(Integer classId, String className) {
        Class existing = classMapper.selectById(classId);
        if (existing == null) return 502;
        Long cnt = classMapper.selectCount(new LambdaQueryWrapper<Class>()
                .eq(Class::getClassName, className));
        if (cnt != null && cnt > 0) return 503;
        existing.setClassName(className);
        classMapper.updateById(existing);
        return 200;
    }

    @Override
    public int delete(Integer classId) {
        Class existing = classMapper.selectById(classId);
        if (existing == null) return 502;
        classMapper.deleteById(classId);
        return 200;
    }
}


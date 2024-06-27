package com.example.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.bean.RolePermission;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Select("SELECT a.role_id, b.permission FROM role_permission a LEFT JOIN permission b ON a.role_id = b.id;")
    List<RolePermission> getAll();
}

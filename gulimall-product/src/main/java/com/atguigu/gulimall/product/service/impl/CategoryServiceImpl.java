package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;


    @Override
    public List<CategoryEntity> queryListTree() {
        //查出所有的分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //组装父子结构返回

        //找到所有的一级分类
        List<CategoryEntity> levelOne = entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0).map(menu -> {
                    menu.setBaby(getBabys(menu,entities));
                    return menu;
                }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))).collect(Collectors.toList());

        List<Integer> list = entities.stream().map(CategoryEntity::getCatLevel).collect(Collectors.toList());
        log.info("catLevelList{}", JSON.toJSONString(list));
        Map<Integer, String> map = entities.stream().collect(Collectors.toMap(CategoryEntity::getCatLevel, CategoryEntity::getName, (k1, k2) -> k2));
        log.info("map{}", JSON.toJSONString(map));
        Map<Integer, CategoryEntity> collect1 = entities.stream().collect(Collectors.toMap(CategoryEntity::getCatLevel, Function.identity(), (k1, k2) -> k2));
        log.info("转map1{}",JSON.toJSONString(collect1));
        Map<Integer, CategoryEntity> collec = entities.stream().collect(Collectors.toMap(CategoryEntity::getCatLevel, Function.identity(), (k1, k2) -> k1));
        log.info("转map2{}",JSON.toJSONString(collec));
        Map<Long, CategoryEntity> collect2 = entities.stream().collect(Collectors.toMap(CategoryEntity::getCatId, Function.identity(), (k1, k2) -> k1));
        log.info("转map{}",JSON.toJSONString(collect2));
        return levelOne;
    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getBabys(CategoryEntity root,List<CategoryEntity> all){
        List<CategoryEntity> babys = all.stream().filter(data -> {
            return data.getParentCid()== root.getCatId();
        }).map(data->{
            //找子菜单
            data.setBaby(getBabys(data,all));
            return data;
        }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))).collect(Collectors.toList());
        return babys;
    }



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
             categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());




        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;

    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2、菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }



}
package com.jtexplorer.entity.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excel.poi.ExcelBoot;
import com.excel.poi.function.ExportFunction;
import com.jtexplorer.entity.enums.RequestEnum;
import com.jtexplorer.util.FileUtil;
import com.jtexplorer.util.JsonResult;
import com.jtexplorer.util.StringUtil;
import com.jtexplorer.util.TimeTools;
import lombok.Data;
import lombok.ToString;
import org.apache.ibatis.session.RowBounds;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * QueryParam class
 *
 * @author 苏友朋
 * @date 2019/06/24 09:41
 */
@Data
public abstract class QueryParam {

    /**
     * 页数
     */
    private Integer page = 1;
    /**
     * 行数
     */
    private Integer limit = 10;

    /**
     * 分组列
     */
    private String groupItem;

    /**
     * 排序列
     */
    private String orderItem;
    /**
     * 排序类型
     */
    private String orderType;
    /**
     * 是否导出
     */
    private String isExport;

    /**
     * 获取分页信息
     *
     * @return RowBounds
     */
    public RowBounds getRowBounds() {
        if (getLimit() == 0) {
            return new RowBounds();
        } else {
            return new RowBounds((this.page - 1) * this.limit, this.limit);
        }
    }

    /**
     * 获取分页信息
     *
     * @return RowBounds
     */
    public <T> Page<T> buildPage(ServiceImpl service) {
        if (StringUtil.isNotEmpty(getIsExport()) && "Y".equals(getIsExport())) {
            setLimit(0);
        }
        Page<T> page = new Page<>(getPage(), getLimit());
        if (page.getSize() == 0) {
            page.setRecords(service.list(getQueryP()));
            page.setTotal(service.count(getQueryP()));
        } else {
            service.getBaseMapper().selectPage(page, getQueryP());
        }
        return page;
    }

    /**
     * 导出查询
     *
     * @return RowBounds
     */
    public <T> Page<T> buildPage(ServiceImpl service, Class clasz) {
        if (StringUtil.isNotEmpty(getIsExport()) && "Y".equals(getIsExport())) {
            setLimit(0);
        }
        Page<T> page = new Page<>(getPage(), getLimit());
        if (page.getSize() == 0) {
            if (StringUtil.isNotEmpty(getIsExport()) && "Y".equals(getIsExport())) {
                // 构建excel导出地址
                buildExcelPath();
                try {
                    ExcelBoot.ExportBuilder(new FileOutputStream(new File(getExcelSavePath())),
                            "Sheet名", clasz)
                            .exportStream(getQueryP(), new ExportFunction<QueryWrapper, T>() {
                                @Override
                                public List<T> pageQuery(QueryWrapper queryWrapper, int i, int i1) {
                                    Page<T> page = new Page<>(i, i1);
                                    service.getBaseMapper().selectPage(page, getQueryP());
                                    return page.getRecords();
                                }

                                @Override
                                public Object convert(T t) {
                                    return t;
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                page.setRecords(service.list(getQueryP()));
                page.setTotal(service.count(getQueryP()));
            }

        } else {
            service.getBaseMapper().selectPage(page, getQueryP());
        }

        return page;
    }


    /**
     * 获取排序字符串
     *
     * @return RowBounds
     */
    public String getOrderString() {
        if (StringUtil.isNotEmpty(getOrderItem()) && StringUtil.isNotEmpty(getOrderType())) {
            return getOrderItem() + " " + getOrderType();
        }
        return null;
    }

    /**
     * 生成排序条件
     */
    public void buildOrderQuery(QueryWrapper query) {
        if (StringUtil.isNotEmpty(getOrderItem())) {
            String[] orderItemStr = getOrderItem().split(",");
            if (StringUtil.isEmpty(getOrderType())) {
                setOrderType("desc");
            }
            for (String item : orderItemStr) {
                String[] items = item.split(" ");
                String itemType = getOrderType();
                if (items.length > 1) {
                    itemType = items[1];
                }
                if ("desc".equals(itemType)) {
                    query.orderByDesc(items[0]);
                } else {
                    query.orderByAsc(items[0]);
                }
            }
        }
    }

    public abstract QueryWrapper getQueryP();

    /**
     * 保存地址
     */
    private String savePath;

    /**
     * 根目录
     */
//    @Value("${myConfig.file.uploadUrl}")
    private String webappPath;

    /**
     * 导出文件地址
     */
    private String excelReturnPath;
    private String excelSavePath;
    private String excelSavePathTemp;
    /**
     * 导入excel文件
     */
    File fileExcel;

    public void buildExcelPath() {
        StringBuilder exportPath = new StringBuilder();
        StringBuilder exportPathTemp = new StringBuilder();
        StringBuilder returnPath = new StringBuilder();
        exportPath.append(webappPath).append(savePath);
        exportPathTemp.append(webappPath).append(savePath);
        returnPath.append("/").append(savePath);
        if (!new File(exportPath.toString()).exists()) {
            //创建目录
            new File(exportPath.toString()).mkdirs();
        }
        exportPath.append(System.currentTimeMillis()).append(".xlsx");
        exportPathTemp.append(System.currentTimeMillis()).append("temp").append(".xlsx");
        returnPath.append(System.currentTimeMillis()).append(".xlsx");
        setExcelReturnPath(returnPath.toString());
        setExcelSavePath(exportPath.toString());
        setExcelSavePathTemp(exportPathTemp.toString());
    }

    /**
     * oee 构建异常查询条件
     *
     * @return boolean
     */
    public void buildSavePath(String webappPath) {
        //获取webapp根目录
//        this.webappPath = session.getServletContext().getRealPath("/") + "WEB-INF/classes/static/";
        this.webappPath = webappPath;
        //项目的目录
        this.savePath = "upload" + "/" + getYearMonthDay() + "/";
    }

    /**
     * 根据日期获取年月日
     *
     * @return 格式化的日期
     */
    private static String getYearMonthDay() {
        try {
            return TimeTools.transformDateFormat(new Date(), "yyyy-MM-dd");
        } catch (ParseException e) {
            return "1954-10-01";
        }
    }

    public JsonResult buildImportExcelFile(MultipartFile file) {
        JsonResult jsonResult = new JsonResult();
        try {
            String fileName = file.getOriginalFilename();
            File folder = new File(getWebappPath() + getSavePath());
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File fileNew = new File(getWebappPath() + getSavePath() + File.separator + fileName);
            if (!fileNew.exists()) {
                fileNew.createNewFile();
            }

            if (StringUtil.isNotEmpty(fileName)) {
                setFileExcel(FileUtil.multipartFileToFile(file, getWebappPath() + getSavePath() + "/import" + file.getOriginalFilename()));
                jsonResult.buildTrueNew();
                return jsonResult;
            } else {
                jsonResult.buildFalseNew(RequestEnum.REQUEST_ERROR_PARAMETER_ERROR, "文件异常");
                return jsonResult;
            }
        } catch (IOException e) {
            e.printStackTrace();
            jsonResult.buildFalseNew(RequestEnum.REQUEST_ERROR_PARAMETER_ERROR, "文件异常" + e.getMessage());
            return jsonResult;
        }
    }

    /**
     * have类参数验证
     *
     * @return boolean
     */
    public boolean paramHave(String param) {
        return StringUtil.isNotEmpty(param) && "Y".equals(param);
    }

}
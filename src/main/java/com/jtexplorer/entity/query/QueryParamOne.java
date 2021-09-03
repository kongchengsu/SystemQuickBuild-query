package com.jtexplorer.entity.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excel.poi.ExcelBoot;
import com.excel.poi.common.Constant;
import com.excel.poi.function.ExportFunction;
import com.jtexplorer.config.ParamStaticConfig;
import com.jtexplorer.entity.enums.RequestEnum;
import com.jtexplorer.util.*;
import lombok.Data;
import org.apache.ibatis.session.RowBounds;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.util.*;

/**
 * QueryParam class
 *
 * @author 苏友朋
 * @date 2019/06/24 09:41
 */
@SuppressWarnings(value = {"rawtypes"})
@Data
public abstract class QueryParamOne<Q extends QueryParamOne, T> {

    private T param;

    private QueryWrapper<T> query;
    private UpdateWrapper<T> update;
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
     * 排序类型 默认是desc
     */
    private String orderType = "asc";
    /**
     * 是否导出
     */
    private String isExport;
    /**
     * 导出是否成功
     */
    private boolean isExportSuccess = false;
    /**
     * Sheet名
     */
    private String sheetName = "sheet";
    /**
     * 下划线（驼峰转下划线时使用）
     */
    public static final char UNDERLINE = '_';
    /**
     * 导出时，将T类对象转为想要的类的对象的方法
     */
    private QueryConvertConsumer<T> convert;
    /**
     * 导出时，将查询出的T类对象的list进行处理的方法
     */
    private QueryConvertListConsumer<T> convertList;
    /**
     * 业务逻辑，QueryPage方法会调用
     */
    private QueryBusinessLogicConsumer BusinessLogic;
    /**
     * 保存地址
     */
    private String savePath;
    /**
     * 根目录
     * 默认classpath
     */
    private String webappPath = Objects.requireNonNull(QueryParamOne.class.getResource("/")).toString().replaceFirst("file:/", "");
//    private String webappPath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("/")).getPath();
    /**
     * 导出文件地址
     */
    private String excelReturnPath;
    private String excelSavePath;
    private List<T> dataList;
    private String excelSavePathTemp;
    /**
     * 导入excel文件
     */
    File fileExcel;

    private static List<String> sqlKey = new ArrayList<>();

    static {
        sqlKey.add("create ");
        sqlKey.add("select ");
        sqlKey.add("from ");
        sqlKey.add("where ");
        sqlKey.add("distinct ");
        sqlKey.add("all ");
        sqlKey.add("and ");
        sqlKey.add("or ");
        sqlKey.add("not ");
        sqlKey.add(";");
        sqlKey.add("insert ");
        sqlKey.add("into ");
        sqlKey.add("table ");
        sqlKey.add("values ");
        sqlKey.add("group ");
        sqlKey.add("by ");
        sqlKey.add("union ");
        sqlKey.add("intersect ");
        sqlKey.add("except ");
        sqlKey.add("is ");
        sqlKey.add("having ");
        sqlKey.add("join ");
        sqlKey.add("delete ");
        sqlKey.add("update ");
        sqlKey.add("drop ");
        sqlKey.add("alter ");
        sqlKey.add("user ");
        sqlKey.add("password ");
        sqlKey.add("SET ");
        sqlKey.add("grant ");
        sqlKey.add("privileges ");
        sqlKey.add("identified ");
        sqlKey.add("REVOKE ");
        sqlKey.add("FLUSH ");
    }

    private Class<T> clazz = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    private Class<Q> clazzQ = (Class<Q>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];


    /**
     * 获取mybatis的分页类对象
     * limit为0的不分页
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
     * 获取mybatisPlus的分页类对象
     * limit为0的不分页
     *
     * @return Page
     */
    public Page<T> getIPage() {
        Page<T> page = new Page<>(getPage(), getLimit());
        if (getLimit() == 0) {
            page.setSize(-1);
        }
        return page;
    }

    /**
     * 验证某参数是否存在（验证是否为Y，是Y的时候返回true）
     *
     * @return Page
     */
    public boolean verifyParamIsY(String param) {
        return "Y".equals(param);
    }

    /**
     * 查询数据
     *
     * @return Page
     */
    public IPage<T> QueryPage(ServiceImpl service) {
        if (getBusinessLogic() != null) {
            getBusinessLogic().convert(this);
        }
        // 拼装查询参数（属性query）
        buildExample();
        // 构建mybatisPlus的分页类对象
        IPage<T> page = getIPage();
        // 根据MyServiceImpl对象查询，该方法查询时，如果page为空，则不分页查询
        return service.page(page, getQuery());
    }

    /**
     * 查询或导出
     *
     * @return RowBounds
     */
    public IPage<T> QueryPage(ServiceImpl service, Class clasz) {
        if (verifyParamIsY(getIsExport())) {
            // 是导出
            // 首先构建文件导出地址
            buildExcelPath();

            try {
                ExcelBoot.ExportBuilder(new FileOutputStream(getExcelSavePath()),
                        getSheetName(), clasz, 500, Constant.DEFAULT_ROW_ACCESS_WINDOW_SIZE, Constant.DEFAULT_RECORD_COUNT_PEER_SHEET, Constant.OPEN_AUTO_COLUM_WIDTH).exportStream(getQuery(), new ExportFunction<QueryWrapper<T>, T>() {
                    @Override
                    public List pageQuery(QueryWrapper queryWrapper, int i, int i1) {
                        IPage<T> page;
                        if (getDataList() != null) {
                            page = new Page<>();
                            page.setRecords(getDataList());
                            page.setTotal(getDataList().size());
                        } else {
                            // 分页查询数据，为了避免查询数据过大，必须分页查询
                            setLimit(i1);
                            setPage(i);
                            page = QueryPage(service);
                        }
                        if (getConvertList() == null) {
                            return page.getRecords();
                        }
                        return getConvertList().convert(page);
                    }

                    @Override
                    public Object convert(T o) {
                        // 对象转化
                        if (getConvert() == null) {
                            return o;
                        }
                        // 转化方法不为空，则返回转化后的对象
                        return getConvert().convert(o);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Page<>();
        } else {
            if (getDataList() != null) {
                Page<T> page = new Page<>();
                page.setRecords(getDataList());
                page.setTotal(getDataList().size());
                return page;
            } else {
                // 查询
                return QueryPage(service);
            }
        }
    }

    public JsonResult QueryJsonResult(ServiceImpl service, Class clasz) {
        JsonResult jsonResult = new JsonResult();
        IPage<T> page = QueryPage(service, clasz);
        if (verifyParamIsY(getIsExport())) {
            jsonResult.buildTrueNew();
            jsonResult.setTip(getExcelReturnPath());
        } else {
            List records = EntityUtil.parentListToChildList(page.getRecords(), clasz);
            if (ListUtil.isNotEmpty(records)) {
                jsonResult.buildTrueNew(page.getTotal(), records);
            } else {
                jsonResult.buildFalseNew(RequestEnum.REQUEST_ERROR_DATABASE_QUERY_NO_DATA);
            }
        }
        return jsonResult;
    }


    public static boolean sqlKeyV(String item) {
        for (String sqlKeyItem : sqlKey) {
            if (item.toLowerCase(Locale.ENGLISH).contains(sqlKeyItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成排序条件
     */
    public void buildOrderQuery(QueryWrapper query) {
        if (StringUtil.isNotEmpty(getOrderItem())) {
            // 按照逗号 分开各排序列
            String[] orderItemStr = getOrderItem().split(",");
            // 逐列处理，携带排序类型的，则使用携带的排序类型，未携带排序类型的，则使用OrderType
            for (String item : orderItemStr) {
                String[] items = item.split(" ");
                String itemType = getOrderType();
                if (items.length > 1) {
                    itemType = items[1];
                }
                items[0] = items[0].replace(" ", "");
                if (sqlKeyV(items[0]) && !items[0].contains(" ")) {
                    // 首先该列是否存在于原始类（数据表）中
                    Field[] fields = clazz.getDeclaredFields();
                    boolean have = false;
                    for (Field field : fields) {
                        String columnName = QueryTypeEnum.camelToUnderline(field.getName(), 1);
                        if (buildOrderItem(query, items[0], columnName, itemType)) {
                            have = true;
                            break;
                        }
                    }
                    if (!have) {
                        // 原始类（数据表）中没有，从注解中找
                        if (clazzQ.isAnnotationPresent(OrderColumn.class)) {
                            OrderColumn joinExample = clazzQ.getDeclaredAnnotation(OrderColumn.class);
                            String[] columnNames = joinExample.columnName();
                            for (String columnName : columnNames) {
                                if (buildOrderItem(query, items[0], columnName, itemType)) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean buildOrderItem(QueryWrapper query, String param, String columnName, String itemType) {
        if (param.equals(columnName)) {
            if ("desc".equals(itemType)) {
                query.orderByDesc(columnName);
            } else {
                query.orderByAsc(columnName);
            }
            return true;
        }
        return false;
    }

    /**
     * 生成excel地址
     */
    public void buildExcelPath() {
        //项目的目录
        buildSavePath();
        this.savePath = "upload" + "/" + getYearMonthDay() + "/";
        StringBuilder exportPath = new StringBuilder();
        StringBuilder exportPathTemp = new StringBuilder();
        StringBuilder returnPath = new StringBuilder();
        exportPath.append(webappPath).append(savePath);
        exportPathTemp.append(webappPath).append(savePath);
        returnPath.append("/").append(savePath);
        if (!new File(exportPath.toString()).exists()) {
            //创建目录
            if (!new File(exportPath.toString()).mkdirs()) {
                return;
            }
        }
        exportPath.append(System.currentTimeMillis()).append(".xlsx");
        exportPathTemp.append(System.currentTimeMillis()).append("temp").append(".xlsx");
        returnPath.append(System.currentTimeMillis()).append(".xlsx");
        setExcelReturnPath(returnPath.toString());
        setExcelSavePath(exportPath.toString());
        setExcelSavePathTemp(exportPathTemp.toString());
    }

    /**
     * 构建根目录
     */
    public void buildSavePath() {
        String webappPath = ParamStaticConfig.getWebappPathStatic(ParamStaticConfig.ConfigParamKeyEnum.uploadUrl).toString();
        if (StringUtil.isNotEmpty(webappPath)) {
            this.webappPath = webappPath;
        }
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

    /**
     * 将MultipartFile转换为属性fileExcel
     */
    public JsonResult buildImportExcelFile(MultipartFile file) {
        JsonResult jsonResult = new JsonResult();
        try {
            String fileName = file.getOriginalFilename();
            File folder = new File(getWebappPath() + getSavePath());
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    return jsonResult.buildFalseNewReturn(RequestEnum.REQUEST_ERROR_SYSTEM_ERROR, "文件路径创建异常");
                }
            }
            File fileNew = new File(getWebappPath() + getSavePath() + File.separator + fileName);
            if (!fileNew.exists()) {
                if (!fileNew.createNewFile()) {
                    return jsonResult.buildFalseNewReturn(RequestEnum.REQUEST_ERROR_SYSTEM_ERROR, "文件创建异常");
                }
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
     * 根据param和本身进行查询参数构建
     */
    public QueryWrapper<T> buildExample() {
        // 在这里new，用于每次调用都能够重新构建查询参数
        query = new QueryWrapper<>();
        update = new UpdateWrapper<>();
        // 首先根据param将全部不为空的项，做成查询参数
        if (getParam() != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                try {
                    if (!"serialVersionUID".equals(field.getName())
                            && StringUtil.isNotEmpty(clazz.getMethod("get" + QueryTypeEnum.upperFirstLatter(field.getName())).invoke(getParam()) != null))
                        QueryTypeEnum.EQ.buildQuery(field, getParam(), query, update);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // 然后根据本身的JoinExample注解，将对应的项，做成查询参数
        Field[] fields = clazzQ.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(JoinExample.class)) {
                JoinExample joinExample = field.getDeclaredAnnotation(JoinExample.class);
                String columnName = joinExample.columnName();
                joinExample.queryType().buildQuery(field, this, columnName, query, update);
            }
        }
        // 排序
        buildOrderQuery(query);
        // 分组
        if (StringUtil.isNotEmpty(groupItem)) {
            query.groupBy(groupItem);
        }
        return query;
    }

}
package com.pro.warehouse.controller;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.pro.warehouse.Service.EntrepotStatusService;
import com.pro.warehouse.Service.ExcelService;
import com.pro.warehouse.Service.LogService;
import com.pro.warehouse.constant.ApplyStatus;
import com.pro.warehouse.constant.Operation;
import com.pro.warehouse.dao.ApplyOutPutRepository;
import com.pro.warehouse.dao.CommonRepository;
import com.pro.warehouse.dao.EntrepotStatusRepository;
import com.pro.warehouse.myexception.StoreException;
import com.pro.warehouse.pojo.ApplyEnter;
import com.pro.warehouse.pojo.ApplyOutPut;
import com.pro.warehouse.pojo.EntrepotStatus;
import com.pro.warehouse.pojo.User;
import com.pro.warehouse.util.PageUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.mail.Store;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Controller
public class ApplyOutController {
    Logger logger = LoggerFactory.getLogger(ApplyOutController.class.getName());
    @Autowired
    private EntrepotStatusRepository entrepotStatusRepository;
    @Autowired
    private ApplyOutPutRepository applyOutPutRepository;
    @Autowired
    private EntrepotStatusService entrepotStatusService;
    @Resource
    private ResourceLoader resourceLoader;
    @Autowired
    private ExcelService<ApplyOutPut> excelService;
    // ??????@Resource????????????JdbcTemplate??????
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CommonRepository<ApplyOutPut> commonRepository;
    @Autowired
    private LogService logService;
    private Integer pagesize = 20;//?????????????????????

    /**
     * @param applyOutPut
     * @param pagenum
     * @param modelMap
     * @return
     */
    @RequestMapping("/applyout-getHistory")
    public String getHistory(ApplyOutPut applyOutPut, int pagenum, ModelMap modelMap) {
        String page = "exit_apply_history";
        if (applyOutPut != null) {
            StringBuffer sql = null;
            try {
                sql = commonRepository.getFiledValues(applyOutPut, pagenum);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            sql.append("Status ='?????????' OR Status ='?????????'");
            int totalpage = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper(ApplyOutPut.class)).size();
            sql.append(" LIMIT " + (pagenum - 1) * pagesize + "," + pagesize);
            List<ApplyOutPut> applyOutPuts = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper(ApplyOutPut.class));
            modelMap.addAttribute("applys", applyOutPuts);
            modelMap.addAttribute("page", pagenum);
            modelMap.addAttribute("totalpage", PageUtil.getTotalPage(totalpage, pagesize));
        } else {
            Pageable pageable = PageRequest.of(pagenum, pagesize);
            Page<ApplyOutPut> pager = applyOutPutRepository.findApplyOutPutByStatus(ApplyStatus.ENSURE, pageable);
            modelMap.addAttribute("applys", pager.getContent());
            modelMap.addAttribute("page", pagenum);
            modelMap.addAttribute("totalpage", pager.getTotalPages());
        }
        return page;
    }

    //????????????????????????????????????????????????
    @RequestMapping("/applyout-getNotAllowed")
    public String getNotAllowed(ApplyOutPut applyOutPut, int pagenum, ModelMap modelMap, HttpServletRequest request) throws Exception {
        String page = "exit_apply";
        //???????????????????????????ID
        User user = (User) request.getSession().getAttribute("user");
        if(user==null){
            throw new StoreException("??????????????????");
        }
        Long userId = user.getId();
        if (applyOutPut != null) {
            StringBuffer sql = null;
            try {
                sql = commonRepository.getFiledValues(applyOutPut, pagenum);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            sql.append("  Status !='?????????' AND Status !='?????????' AND applyPersonId = '" + user.getUsername()+"'");
            int totalpage = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper(ApplyOutPut.class)).size();
            sql.append(" LIMIT " + (pagenum - 1) * pagesize + "," + pagesize);
            List<ApplyOutPut> applyEnters = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper(ApplyOutPut.class));
            modelMap.addAttribute("applys", applyEnters);
            modelMap.addAttribute("page", pagenum);
            modelMap.addAttribute("totalpage", PageUtil.getTotalPage(totalpage, pagesize));
        } else {
            Pageable pageable = PageRequest.of(pagenum, pagesize);
            Page<ApplyOutPut> pager = applyOutPutRepository.findApplyOutPutByStatusNot(ApplyStatus.ENSURE, pageable);
            modelMap.addAttribute("applys", pager.getContent());
            modelMap.addAttribute("page", pagenum);
            modelMap.addAttribute("totalpage", pager.getTotalPages());
        }
        modelMap.addAttribute("username", user.getUsername());
        return page;
    }


    //?????????????????????
    @RequestMapping("/applyout-getToBeEnsured")
    public String getToBeEnsured(ApplyOutPut applyOutPut, int pagenum, ModelMap modelMap, HttpServletRequest request) {
        String page = "exit_apply_wait";
        //???????????????????????????ID
        if (applyOutPut != null) {
            StringBuffer sql = null;
            try {
                sql = commonRepository.getFiledValues(applyOutPut, pagenum);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            sql.append("  Status !='?????????' AND Status != '?????????' AND Status != '?????????'");
            int totalpage = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper(ApplyOutPut.class)).size();
            sql.append(" LIMIT " + (pagenum - 1) * pagesize + "," + pagesize);
            List<ApplyOutPut> applyEnters = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper(ApplyOutPut.class));
            modelMap.addAttribute("applys", applyEnters);
            modelMap.addAttribute("page", pagenum);
            modelMap.addAttribute("totalpage", PageUtil.getTotalPage(totalpage, pagesize));
        } else {
            Pageable pageable = PageRequest.of(pagenum, pagesize);
            Page<ApplyOutPut> pager = applyOutPutRepository.findApplyOutPutByStatusNot(ApplyStatus.ENSURE, pageable);
            modelMap.addAttribute("applys", pager.getContent());
            modelMap.addAttribute("page", pagenum);
            modelMap.addAttribute("totalpage", pager.getTotalPages());
        }

        return page;
    }

    /**
     * ??????????????????  ?????????????????????
     */
    @RequestMapping(value = "/applyout-addapply", method = {RequestMethod.GET, RequestMethod.POST})
    public String saveApply(ApplyOutPut applyOutPut, BindingResult bindingResult, HttpServletRequest request, ModelMap modelMap) throws Exception {
        User user = (User) request.getSession().getAttribute("user");
        if(user==null){
            throw new StoreException("??????????????????");
        }
        Long userId = user.getId();
        applyOutPut.setApplyPersonid(user.getUsername());
        List<EntrepotStatus> entrots = entrepotStatusRepository.findByEnterCodeAndMaterialCode(applyOutPut.getEnterCode(),applyOutPut.getMaterialCode().trim().replaceAll(" +","%"));
        logger.debug("?????????????????????????????????"+ applyOutPut.getEnterCode() + "  " + applyOutPut.getMaterialCode());
        logger.debug("???????????????"+entrots);
        if (entrots.size() > 0) {
            applyOutPut.setProduceDate(entrots.get(0).getProduceDate());
            applyOutPut.setStatus("?????????");
            applyOutPut.setApplyDate(new Date());
            //??????
            applyOutPutRepository.save(applyOutPut);
            request.getSession().setAttribute("message", "????????????");
        } else {
            request.getSession().setAttribute("message", "????????????????????????????????????");
        }
        logService.saveOpLog(user.getUsername(), Operation.APPLY_OUT.getOperation(),"??????", JSON.toJSONString(applyOutPut));

        return "redirect:/applyout-getNotAllowed?pagenum=1";
    }

    /**
     * ??????????????????(??????????????????????????????????????????????????????id??????????????????????????????)
     */
    @RequestMapping("/applyout-updateStatus")
    public String ensureApply(int id, HttpServletRequest request, ModelMap modelMap, HttpSession session) throws Exception {

        String page = "exit_apply_wait";
        //??????id??????????????????
        ApplyOutPut output = applyOutPutRepository.findApplyOutPutById(id);
        String result = "??????";
        String detail = "";
        //????????????
        String materialCode = output.getMaterialCode();
        //???????????????????????????????????????????????????????????????????????????????????????????????????????????????%,???????????????????????????????????????
        materialCode = materialCode.trim().replaceAll(" +","%");
        //????????????
        String code = output.getEnterCode();
        //output.getp
        //????????????????????????
        EntrepotStatus entrepotTarget = null;
        List<EntrepotStatus> entrepotStatus = entrepotStatusRepository.findByEnterCodeAndMaterialCode(code, materialCode);
        if(entrepotStatus.isEmpty()){
            throw new StoreException("???????????????????????????????????????????????????");
        }
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return "user-login";
        }
        logger.debug("?????????????????????????????????enterid"+code+"??????"+materialCode);
        logger.debug("???????????????"+entrepotStatus);
        if(entrepotStatus.size()==0||entrepotStatus==null){
            output.setStatus(ApplyStatus.REFUSED);
            output.setApplyPersonid(user.getUsername());
            output.setApplyDate(new Date());
            request.getSession().setAttribute("message","????????????????????????");
            detail = "????????????????????????";
        }
        //?????????????????????????????????????????????????????????????????????????????????
        if (output.getDemandName().equals(entrepotStatus.get(0).getSupplyName())) {
            //???????????????
            EntrepotStatus badGodds = null;
            for(EntrepotStatus status:entrepotStatus){
                if("?????????".equals(status.getGoodsStatus())){
                    badGodds = status;
                }
            }
            if(badGodds.getTotalSize()>output.getSize()){
              //?????????????????????????????????
                badGodds.setTotalSize(badGodds.getTotalSize()-output.getSize());
                //????????????
                entrepotStatusRepository.save(badGodds);
                //????????????????????????
                output.setApplyDate(new Date());
                output.setStatus(ApplyStatus.TURN_BACK);
                request.getSession().setAttribute("message","??????????????????:??????-"+output.getSize()+",???????????????-"+badGodds.getTotalSize());
                result = "??????";
                detail = "??????????????????:??????-"+output.getSize()+",???????????????-"+badGodds.getTotalSize();
            }else if(badGodds.getTotalSize()==output.getSize()){
                //??????????????????????????????????????????
                entrepotStatusRepository.delete(badGodds);
                output.setStatus(ApplyStatus.TURN_BACK);
                request.getSession().setAttribute("message","??????????????????");
                result = "??????";
                detail = "???????????????";
            }else{
                //????????????????????????????????????
                output.setStatus(ApplyStatus.REFUSED);
                request.getSession().setAttribute("message","??????????????????????????????????????????");
                detail = "??????????????????????????????????????????";
            }
            //?????????????????????????????????,????????????????????????
            //????????????????????????
            output.setEnsurePersonid(user.getUsername());
            output.setApplyDate(new Date());
            applyOutPutRepository.save(output);
        }else{
            //????????????
            //????????????
            EntrepotStatus goodGodds = null;
            for(EntrepotStatus status:entrepotStatus){
                if("??????".equals(status.getGoodsStatus())){
                    goodGodds = status;
                }
            }
            if(null==goodGodds){
                request.getSession().setAttribute("message","???????????????????????????");
                return "redirect:/applyout-getToBeEnsured?pagenum=1";
            }
            if(goodGodds.getTotalSize()>output.getSize()){
                //?????????????????????????????????
                goodGodds.setTotalSize(goodGodds.getTotalSize()-output.getSize());
                //????????????
                entrepotStatusRepository.save(goodGodds);
                //????????????????????????
                output.setStatus(ApplyStatus.ENSURE);
                request.getSession().setAttribute("message","??????????????????");
                result="??????";
                detail="??????????????????:";
            }else if(goodGodds.getTotalSize()==output.getSize()){
                //??????????????????????????????????????????
                entrepotStatusRepository.delete(goodGodds);
                output.setStatus(ApplyStatus.ENSURE);
                request.getSession().setAttribute("message","??????????????????");
                result="??????";
                detail="??????????????????:";
            }else{
                //????????????????????????????????????
                output.setStatus(ApplyStatus.REFUSED);
                request.getSession().setAttribute("message","??????????????????????????????????????????");
                detail="??????????????????????????????????????????";
            }
            //?????????????????????????????????,????????????????????????
            //????????????????????????
            output.setEnsurePersonid(user.getUsername());
            output.setApplyDate(new Date());
            applyOutPutRepository.save(output);
            logService.saveOpLog(user.getUsername(), Operation.ENSURE_ENTER.getOperation(),result, detail+JSON.toJSONString(output));
        }
        return "redirect:/applyout-getToBeEnsured?pagenum=1";
    }



    /**
     * ??????
     */
    @RequestMapping("/applyout-turndown")
    public String turndown(int id, HttpServletRequest request) {
        logger.debug("??????ID???" + id + "????????????");
        ApplyOutPut output = applyOutPutRepository.findApplyOutPutById(id);
        output.setStatus(ApplyStatus.REFUSED);
        User user = (User) request.getSession().getAttribute("user");
        output.setEnsurePersonid(user.getUsername());
        //??????
        applyOutPutRepository.save(output);
        logService.saveOpLog(user.getUsername(), Operation.REFUSE_OUT.getOperation(),"??????", JSON.toJSONString(output));
        return "redirect:/applyout-getToBeEnsured?pagenum=1";
    }

    /**
     * ??????ID??????
     */
    @RequestMapping(value = "/applyout-deleteById",method = {RequestMethod.POST,RequestMethod.GET})
    public String deleteApplyById(int enterId, HttpServletRequest request) throws StoreException {
        User user = (User) request.getSession().getAttribute("user");
        if(user==null){
            throw new StoreException("??????????????????");
        }
        ApplyOutPut applyOutPut = applyOutPutRepository.findApplyOutPutById(enterId);
        applyOutPutRepository.delete(applyOutPut);
        logService.saveOpLog(user.getUsername(), Operation.DELETE_APPLY_OUT.getOperation(),"??????", JSON.toJSONString(applyOutPut));
        return "redirect:/applyout-getNotAllowed?pagenum=1";
    }


    /**
     * ??????ID??????
     */
    @RequestMapping(value = "/applyout-his-deleteById",method = {RequestMethod.POST,RequestMethod.GET})
    public String deleteHisApplyById(int enterId, HttpServletRequest request) throws StoreException {
        User user = (User) request.getSession().getAttribute("user");
        if(user==null){
            throw new StoreException("??????????????????");
        }
        ApplyOutPut applyOutPut = applyOutPutRepository.findApplyOutPutById(enterId);
        applyOutPutRepository.delete(applyOutPut);
        logService.saveOpLog(user.getUsername(), Operation.DELETE_APPLY_OUT_HIS.getOperation(),"??????", JSON.toJSONString(applyOutPut));
        return "redirect:/applyout-getHistory?pagenum=1";
    }


    /**
     * ????????????
     */
    @RequestMapping(value = "/applyout-batchApply")
    public String batchApply(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws InstantiationException, IllegalAccessException, StoreException {
        List<ApplyOutPut> applyOutPuts = excelService.ImportExcelService(file, new ApplyOutPut());
        logger.debug(new Date()+"???????????????????????????"+new Gson().toJson(applyOutPuts));
        String success = "";
        User user1 = (User) request.getSession().getAttribute("user");
        if(user1==null){
            throw new StoreException("??????????????????");
        }
        for(ApplyOutPut applyOutPut:applyOutPuts){
            User user = (User) request.getSession().getAttribute("user");
            applyOutPut.setApplyPersonid(user.getUsername());
            applyOutPut.setApplyDate(new Date());
            applyOutPut.setStatus("?????????");
            applyOutPutRepository.save(applyOutPut);
            success = success+applyOutPut.getEnterCode()+"--";
        }
        logService.saveOpLog(user1.getUsername(), Operation.APPLY_OUT_BATCH.getOperation(),"??????", JSON.toJSONString(applyOutPuts));
        request.getSession().setAttribute("message","?????????????????????(????????????)???"+success);
        return "redirect:/applyout-getNotAllowed?pagenum=1";
    }

    /**
     * ??????????????????
     * @param response
     * @param req
     * @throws IOException
     */
    @RequestMapping(value = "/applyout-downloadExcel")
    public void doloadExcel(HttpServletResponse response, HttpServletRequest req) throws IOException {
        InputStream inputStream = null;
        ServletOutputStream servletOutputStream = null;
        try {
            String filename = "????????????????????????.xlsx";
            String path = "files/????????????????????????.xlsx";
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:"+path);

            response.setContentType("application/vnd.ms-excel");
            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.addHeader("charset", "utf-8");
            response.addHeader("Pragma", "no-cache");
            String encodeName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodeName + "\"; filename*=utf-8''" + encodeName);

            inputStream = resource.getInputStream();
            servletOutputStream = response.getOutputStream();
            IOUtils.copy(inputStream, servletOutputStream);
            response.flushBuffer();
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (servletOutputStream != null) {
                    servletOutputStream.close();
                    servletOutputStream = null;
                }
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
                // ??????jvm??????????????????
                System.gc();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * ??????
     */
    @RequestMapping(value = "/applyout-search", method = {RequestMethod.GET, RequestMethod.POST})
    public String doSearch(ApplyOutPut outPut, ModelMap modelMap, HttpServletRequest request,@RequestParam(value = "pagenum", required = false)Integer pagenum) {
        String searchItem = request.getParameter("searchItem");
        String searchValue = request.getParameter("searchValue");
        //Integer pagenum = Integer.parseInt(request.getParameter("pagenum"));
        Integer type = Integer.parseInt(request.getParameter("type"));
        String page = "exit_apply_wait";
        System.out.print(searchItem+"   "+searchValue);
        StringBuffer sql = null;
        try {
            sql = commonRepository.getFiledValues(outPut, pagenum);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if(searchValue!=null||!"".equals(searchValue)) {
            if(type==1) {
                page = "exit_apply_wait";
                sql.append(searchItem + " like '%" + searchValue + "%' AND Status !='" + "?????????'");
            }else{
                page = "exit_apply_history";
                sql.append(searchItem + " like '%" + searchValue + "%' AND Status ='" + "?????????'");
            }
        }else{
            sql.append(" 1 = 1");
        }
        int totalpage = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper(ApplyOutPut.class)).size();
        List<ApplyEnter> applyouts = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper(ApplyOutPut.class));
        logger.debug("????????????????????????" + applyouts);
        modelMap.addAttribute("applys", applyouts);
        modelMap.addAttribute("searchValue",searchValue);
        modelMap.addAttribute("searchItem",searchItem);
        modelMap.addAttribute("totalpage", PageUtil.getTotalPage(totalpage, pagesize));

        return page;
    }
}
